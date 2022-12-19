import com.soywiz.klock.TimeSpan
import com.soywiz.korau.sound.Sound
import com.soywiz.korau.sound.readSound
import com.soywiz.korev.Key
import com.soywiz.korge.input.keys
import com.soywiz.korge.particle.ParticleEmitter
import com.soywiz.korge.particle.readParticleEmitter
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.launch
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.lang.cancel
import com.soywiz.korma.geom.*
import debug.Debug
import entity.Asteroid
import entity.Earth
import entity.MassObject
import entity.Ship
import kotlinx.coroutines.GlobalScope
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt

class Game(val stage: Stage, val container: Container, val onEnd: (Game) -> Unit) {
    private var nextWaveCountdown: Int = 20
    var wave = 1
    val resources = mutableMapOf<String, Bitmap>()
    val particles = mutableMapOf<String, ParticleEmitter>()
    val sounds = mutableMapOf<String, Sound>()
    val asteroids = mutableSetOf<Asteroid>()
    lateinit var playerShip: Ship
    lateinit var earth: Earth
    var running = true
    var ammo = 10
    var asteroidsKilled = 0
    var tanksCollected = mutableSetOf<Asteroid>()
    var score = 0
        get() = ((wave - 1) * 100) + (asteroidsKilled * 50) + (tanksCollected.size * 100)

    suspend fun start() {
        loadResources()
        setupEarth()
        setupPlayerShip()
//        setupAsteroids(15)
        setupAsteroids(0)
        setupDisplay()

        stage.addFixedUpdater(TimeSpan(1000.0)) {
            if (running) {
                if (nextWaveCountdown <= 0) {
                    nextWaveCountdown = 20
                    wave++
                    ammo += (3 * wave) * 2 / 3

                    setupAsteroids(3 * wave)
                    setupFuelTanks(wave)
                } else nextWaveCountdown--
            }
        }
    }

    private suspend fun loadResources() {
        resources += "ship" to resourcesVfs["ship_2.png"].readBitmap()
        resources += "bullet" to resourcesVfs["bullet.png"].readBitmap()
        resources += "earth" to resourcesVfs["earth.png"].readBitmap()
        resources += "asteroid_0" to resourcesVfs["asteroid_0.png"].readBitmap()
        resources += "asteroid_1" to resourcesVfs["asteroid_1.png"].readBitmap()
        resources += "asteroid_2" to resourcesVfs["asteroid_2.png"].readBitmap()
        resources += "tank" to resourcesVfs["tank.png"].readBitmap()
        particles += "thrust" to resourcesVfs["particle/thurst/particle.pex"].readParticleEmitter()
        sounds += "thrust" to resourcesVfs["heavy_steam.wav"].readSound()
        sounds += "torque" to resourcesVfs["heavy_steam.wav"].readSound()
        sounds += "shoot" to resourcesVfs["shoot.wav"].readSound()
        sounds += "breaking" to resourcesVfs["breaking.wav"].readSound()
        sounds += "crash" to resourcesVfs["crash.wav"].readSound()
        sounds += "tank" to resourcesVfs["tank.wav"].readSound()
    }

    private suspend fun setupPlayerShip() {
        val shipSprite: Container = Container()
            .position(earth.sprite.pos.x - earth.sprite.width * 2, earth.sprite.pos.y)
        shipSprite.sprite(resources["ship"] ?: error("Could not find ship resource")) {
            anchor(.5, .5)
        }
        val thrustParticle = particles["thrust"] ?: error("Could not find thrust particle")
        val thrustSound = sounds["thrust"]!!.apply { pitch -= 1.0 }
        val torqueSound = sounds["torque"]!!
        val shootSound = sounds["shoot"]!!
        playerShip = Ship(shipSprite, thrustParticle, thrustSound, torqueSound, shootSound)
        playerShip.yVel += earth.getOrbitalVelocity(playerShip)
        playerShip.rVel = -0.02
        installShipControls()
        installGravity(playerShip)
        container.addChild(shipSprite)
        earth.sprite.addUpdater {
            if (running) {
                if (playerShip.sprite.pos.distanceTo(earth.sprite.pos) > (stage?.unscaledWidth ?: Double.MAX_VALUE) / 1.5 ||
                        playerShip.sprite.pos.distanceTo(earth.sprite.pos) < earth.sprite.width / 2) {
                    destroyPlayer()
                }
            }
        }
    }

    private suspend fun installShipControls() {
        val ks = stage.input.keys
        earth.sprite.addUpdater {
            if (running) {
                if (ks[Key.LEFT] || ks[Key.A] && playerShip.fuel > 0) {
                    if (playerShip.torqueVolume < 0.2) playerShip.torqueVolume += 0.1
                    playerShip.thrustLeft()
                    playerShip.frontRightThrust.emitting = true
                    playerShip.backLeftThrust.emitting = true
                } else {
                    playerShip.frontRightThrust.emitting = false
                    playerShip.backLeftThrust.emitting = false
                }
                if (ks[Key.RIGHT] || ks[Key.D] && playerShip.fuel > 0) {
                    if (playerShip.torqueVolume < 0.2) playerShip.torqueVolume += 0.1
                    playerShip.thrustRight()
                    playerShip.frontLeftThrust.emitting = true
                    playerShip.backRightThrust.emitting = true
                } else {
                    playerShip.frontLeftThrust.emitting = false
                    playerShip.backRightThrust.emitting = false
                }
                if (!ks[Key.LEFT] && !ks[Key.A] && !ks[Key.RIGHT] && !ks[Key.D]) {
                    if (playerShip.torqueVolume > 0.0) playerShip.torqueVolume -= 0.1
                }

                if (ks[Key.UP] || ks[Key.W] && playerShip.fuel > 0) {
                    if (playerShip.thrustVolume < 1.0) playerShip.thrustVolume += 0.1
                    playerShip.thrustForward()
                    playerShip.forwardThrust.emitting = true
                } else {
                    if (playerShip.thrustVolume > 0.0) playerShip.thrustVolume -= 0.1
                    playerShip.forwardThrust.emitting = false
                }
            }
        }
        playerShip.sprite.keys {
            up(Key.SPACE) {
                if (ammo > 0) {
                    ammo--
                    playerShip.fire()
                    launchImmediately(stage.coroutineContext) {
                        fireProjectile()
                    }
                }
            }
        }
    }

    private suspend fun fireProjectile() {
        // createProjectileObject
        // install collision detection
        // set velocity vector
        // add gravity
        // add offScreenDeath
        val breakSound = sounds["breaking"]!!.apply { volume = 0.5 }

        val bulletSprite = Sprite(resources["bullet"] ?: error("Could not find bullet resource"))
                .anchor(0.5, 0.5)
                .scale(2.0, 2.0)
                .position(playerShip.sprite.pos)
                .rotation(playerShip.sprite.rotation + Angle.fromDegrees(90))
        val bullet = MassObject(mass = 5.0, sprite = bulletSprite).apply {
            xVel = playerShip.xVel
            yVel = playerShip.yVel
            rVel = playerShip.rVel + 0.5
        }
        bullet.applyForce(0.5, playerShip.sprite.rotation)
        installGravity(bullet)
        stage.addChildAt(bulletSprite, container.getChildIndex(playerShip.sprite) - 1)

        bullet.sprite.addUpdater {
            if (running) {
                asteroids.filter { bullet.sprite.collidesWith(it.sprite) }
                        .forEach {
                            GlobalScope.launch {
                                breakSound.play()
                            }
                            it.hit()
                            asteroids -= it
                            asteroidsKilled++
                            bulletSprite.addFixedUpdater(TimeSpan(100.0)) {
                                bulletSprite.alpha = alpha - 0.2
                                if (alpha < 0.0) {
                                    // Destroy
                                    bulletSprite.removeFromParent()
                                    bulletSprite.removeAllComponents()
                                }
                            }
                        }
                if (bullet.sprite.pos.distanceTo(earth.sprite.pos) > (stage?.unscaledWidth ?: 0.0) / 2.0) {
                    // Destroy
                    bulletSprite.removeFromParent()
                    bulletSprite.removeAllComponents()
                }
            }
        }
    }

    private fun setupEarth() {
        val earthSprite = Sprite(resources["earth"] ?: error("Could not find earth resource"))
                .anchor(0.5, 0.5)
                .position(container.unscaledWidth / 2, container.unscaledHeight / 2)
        earth = Earth(earthSprite)
        container.addChild(earthSprite)
    }

    private fun installGravity(massObject: MassObject) {
        container.addUpdater {
            if (running) {
                val gravForce = earth.getGravForce(massObject)
                gravForce.x = gravForce.x * it.milliseconds
                gravForce.y = gravForce.y * it.milliseconds
                massObject.applyForce(gravForce)
            }
        }
    }

    private fun setupAsteroids(numberOfAsteroids: Int) {
        // TODO: Refactor into asteroid field
        val asteroidPos = sequence<Point> {
            while (true) {
                val pos = Point(Random.nextInt(stage.unscaledWidth.toInt()), Random.nextInt(stage.unscaledHeight.toInt()))
                if (pos.distanceTo(earth.sprite.pos) > earth.sprite.width &&
                        pos.distanceTo(earth.sprite.pos) < stage.unscaledWidth / 2 &&
                        pos.distanceTo(playerShip.sprite.pos) > playerShip.sprite.width) yield(pos)
            }
        }.iterator()
        repeat(numberOfAsteroids) {
            val asteroidSprite = Sprite(resources["asteroid_${Random.nextInt(3)}"]
                    ?: error("Could not find asteroid resource"))
                    .position(asteroidPos.next())
                    .anchor(0.5, 0.5)
                    .alpha(0.0)

            val nAsteroid = Asteroid(Random.nextInt(2000..2001).toDouble(), asteroidSprite)
            val phaseIn = asteroidSprite.addFixedUpdater(TimeSpan(100.0)) {
                if (alpha < 1.0) alpha += 0.1
            }
            val orbitAngle = nAsteroid.sprite.pos.angleTo(earth.sprite.pos).plus(Angle.fromDegrees(90))
            val orbitVelocity = earth.getOrbitalVelocity(nAsteroid) * (Random.nextDouble(0.5, 1.0))
            nAsteroid.xVel = cos(orbitAngle) * orbitVelocity
            nAsteroid.yVel = sin(orbitAngle) * orbitVelocity
            nAsteroid.rVel = (Random.nextDouble() - Random.nextDouble()) / 4

            installGravity(nAsteroid)

            container.addChild(nAsteroid.sprite)
            asteroids += nAsteroid

            nAsteroid.sprite.addUpdater {
                if (running) {
                    if (alpha >= 1.0) phaseIn.cancel()
                    if (nAsteroid.sprite.pos.distanceTo(playerShip.sprite.pos) < asteroidSprite.width) {
                        destroyPlayer()
                    }
                }
            }
        }
    }

    fun setupFuelTanks(n: Int) {
        val tankPos = sequence<Point> {
            while (true) {
                val pos = Point(Random.nextInt(stage.unscaledWidth.toInt()), Random.nextInt(stage.unscaledHeight.toInt()))
                if (pos.distanceTo(earth.sprite.pos) > earth.sprite.width &&
                        pos.distanceTo(earth.sprite.pos) < stage.unscaledWidth / 2 &&
                        pos.distanceTo(playerShip.sprite.pos) > playerShip.sprite.width) yield(pos)
            }
        }.iterator()
        repeat(n) {
            val tankSprite = Sprite(resources["tank"]
                    ?: error("Could not find tank resource"))
                    .position(tankPos.next())
                    .anchor(0.5, 0.5)
                    .alpha(0.0)
                    .scale(1.5, 1.5)

            val nTank = Asteroid(Random.nextInt(2000..2001).toDouble(), tankSprite)
            val phaseIn = tankSprite.addFixedUpdater(TimeSpan(100.0)) {
                if (alpha < 1.0) alpha += 0.1
            }
            val orbitAngle = nTank.sprite.pos.angleTo(earth.sprite.pos).plus(Angle.fromDegrees(90))
            val orbitVelocity = earth.getOrbitalVelocity(nTank) * (Random.nextDouble(0.5, 1.0))
            nTank.xVel = cos(orbitAngle) * orbitVelocity
            nTank.yVel = sin(orbitAngle) * orbitVelocity
            nTank.rVel = (Random.nextDouble() - Random.nextDouble()) / 4

            installGravity(nTank)

            container.addChild(nTank.sprite)
            asteroids += nTank

            nTank.sprite.addUpdater {
                if (running) {
                    if (alpha >= 1.0) phaseIn.cancel()
                    if (nTank.sprite.pos.distanceTo(playerShip.sprite.pos) < tankSprite.width && !tanksCollected.contains(nTank)) {
                        nTank.hit()
                        playerShip.fuel += playerShip.startingFuel / 3
                        if (playerShip.fuel > playerShip.startingFuel) playerShip.fuel = playerShip.startingFuel
                        tanksCollected.add(nTank)
                        ammo += 5
                        GlobalScope.launch {
                            sounds["tank"]!!.play()
                        }
                    }
                }
            }
        }
    }

    private fun destroyPlayer() {
        GlobalScope.launch {
            sounds["crash"]!!.play()
        }
        running = false
        playerShip.torqueVolume = 0.0
        playerShip.thrustVolume = 0.0
        onEnd(this)
    }

    private fun setupDisplay() {
        val debug = Debug(container)
        debug.track { "Wave: ${wave}" }
        debug.track { "Next Wave in: ${nextWaveCountdown}" }
        debug.track { "Score: ${score}" }
        debug.textLine("")
        debug.track { "Fuel: ${(playerShip.fuel / 30.0).roundToInt()}%" }
        debug.track { "Ammo: $ammo" }
    }
}
