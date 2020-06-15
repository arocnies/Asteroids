import com.soywiz.klock.Date
import com.soywiz.klock.TimeSpan
import com.soywiz.korau.sound.readSound
import com.soywiz.korev.Key
import com.soywiz.korev.keys
import com.soywiz.korge.animate.play
import com.soywiz.korge.input.keys
import com.soywiz.korge.particle.ParticleEmitter
import com.soywiz.korge.particle.readParticle
import com.soywiz.korge.time.TimerComponents
import com.soywiz.korge.time.timers
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.*
import debug.Debug
import entity.Asteroid
import entity.Earth
import entity.MassObject
import entity.Ship
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt

class Game(val stage: Stage, val container: Container, val onEnd: (Game) -> Unit) {
    private var nextWaveCountdown: Int = 20
    var wave = 1
    val resources = mutableMapOf<String, Bitmap>()
    val particles = mutableMapOf<String, ParticleEmitter>()
    val asteroids = mutableSetOf<Asteroid>()
    lateinit var playerShip: Ship
    lateinit var earth: Earth
    var score = 0
    var running = true
    var ammo = 10
    var asteroidsKilled = 0

    suspend fun start() {
        loadResources()
        setupEarth()
        setupPlayerShip()
        setupAsteroids(15)
        setupDisplay()

        stage.addFixedUpdater(TimeSpan(1000.0)) {
            if (running) {
                if (nextWaveCountdown <= 0) {
                    nextWaveCountdown = 20
                    wave++
                    ammo += (3 * wave) * 2 / 3

                    setupAsteroids(3 * wave)
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
        particles += "thrust" to resourcesVfs["particle/thurst/particle.pex"].readParticle()
    }

    private suspend fun setupPlayerShip() {
        val shipSprite = Sprite(resources["ship"] ?: error("Could not find ship resource"))
                .anchor(.5, .5)
                .position(earth.sprite.pos.x - earth.sprite.width, earth.sprite.pos.y)
        val thrustParticle = particles["thrust"] ?: error("Could not find thrust particle")
        val thrustSound = resourcesVfs["heavy_steam.wav"].readSound().apply { pitch -= 1.0 }
        val torqueSound = resourcesVfs["heavy_steam.wav"].readSound()
        val shootSound = resourcesVfs["shoot.wav"].readSound()
        playerShip = Ship(shipSprite, thrustParticle, thrustSound, torqueSound, shootSound)
        playerShip.yVel += earth.getOrbitalVelocity(playerShip)
        playerShip.rVel = -0.05
        installShipControls()
        installGravity(playerShip)
        container.addChild(shipSprite)
        earth.sprite.addUpdater {
            if (running) {
                if (playerShip.sprite.pos.distanceTo(earth.sprite.pos) > (stage?.width ?: 0.0) / 2.0 ||
                        playerShip.sprite.pos.distanceTo(earth.sprite.pos) < earth.sprite.width / 2) {
                    destroyPlayer()
                }
            }
        }
    }

    private suspend fun installShipControls() {
        val ks = KeyState(stage)
        earth.sprite.addUpdater {
            if (running) {
                if (ks[Key.LEFT] || ks[Key.A] && playerShip.fuel > 0) {
                    if (playerShip.torqueSoundChannel.volume < 0.2) playerShip.torqueSoundChannel.volume += 0.1
                    playerShip.thrustLeft()
                    playerShip.frontRightThrust.emitting = true
                    playerShip.backLeftThrust.emitting = true
                } else {
                    playerShip.frontRightThrust.emitting = false
                    playerShip.backLeftThrust.emitting = false
                }
                if (ks[Key.RIGHT] || ks[Key.D] && playerShip.fuel > 0) {
                    if (playerShip.torqueSoundChannel.volume < 0.2) playerShip.torqueSoundChannel.volume += 0.1
                    playerShip.thrustRight()
                    playerShip.frontLeftThrust.emitting = true
                    playerShip.backRightThrust.emitting = true
                } else {
                    playerShip.frontLeftThrust.emitting = false
                    playerShip.backRightThrust.emitting = false
                }
                if (!ks[Key.LEFT] && !ks[Key.A] && !ks[Key.RIGHT] && !ks[Key.D]) {
                    if (playerShip.torqueSoundChannel.volume > 0.0) playerShip.torqueSoundChannel.volume -= 0.1
                }

                if (ks[Key.UP] || ks[Key.W] && playerShip.fuel > 0) {
                    if (playerShip.thrustSoundChannel.volume < 1.0) playerShip.thrustSoundChannel.volume += 0.1
                    playerShip.thrustForward()
                    playerShip.forwardThrust.emitting = true
                } else {
                    if (playerShip.thrustSoundChannel.volume > 0.0) playerShip.thrustSoundChannel.volume -= 0.1
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
        val breakSound = resourcesVfs["breaking.wav"].readSound()

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
                            breakSound.play()
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
            }
        }
    }

    private fun setupEarth() {
        val earthSprite = Sprite(resources["earth"] ?: error("Could not find earth resource"))
                .anchor(0.5, 0.5)
                .position(container.width / 2, container.height / 2)
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
                val pos = Point(Random.nextInt(stage.width.toInt()), Random.nextInt(stage.height.toInt()))
                if (pos.distanceTo(earth.sprite.pos) > earth.sprite.width &&
                        pos.distanceTo(earth.sprite.pos) < stage.width / 2 &&
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

    private fun destroyPlayer() {
        running = false
        playerShip.torqueSoundChannel.volume = 0.0
        playerShip.thrustSoundChannel.volume = 0.0
        onEnd(this)
    }

    private fun setupDisplay() {
        val debug = Debug(container)
        debug.track { "Wave: ${wave}" }
        debug.track { "Next Wave in: ${nextWaveCountdown}" }
        debug.textLine("")
        debug.track { "Fuel: ${(playerShip.fuel / 30.0).roundToInt()}%" }
        debug.track { "Ammo: $ammo" }
    }
}
