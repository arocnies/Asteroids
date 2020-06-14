import com.soywiz.korau.sound.readSound
import com.soywiz.korev.Key
import com.soywiz.korev.keys
import com.soywiz.korge.animate.play
import com.soywiz.korge.particle.ParticleEmitter
import com.soywiz.korge.particle.readParticle
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.format.readBitmap
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
    val resources = mutableMapOf<String, Bitmap>()
    val particles = mutableMapOf<String, ParticleEmitter>()
    val asteroids = mutableSetOf<Asteroid>()
    lateinit var playerShip: Ship
    lateinit var earth: Earth
    var score = 0

    suspend fun start() {
        loadResources()
        setupEarth()
        setupPlayerShip()
        setupAsteroids(15)
        setupDisplay()
    }

    private suspend fun loadResources() {
        resources += "ship" to resourcesVfs["ship_2.png"].readBitmap()
        resources += "bullet" to resourcesVfs["bullet.png"].readBitmap()
        resources += "earth" to resourcesVfs["earth.png"].readBitmap()
        resources += "asteroid" to resourcesVfs["asteroid_0.png"].readBitmap()
        particles += "thrust" to resourcesVfs["particle/thurst/particle.pex"].readParticle()
    }

    private suspend fun setupPlayerShip() {
        val shipSprite = Sprite(resources["ship"] ?: error("Could not find ship resource"))
                .anchor(.5, .5)
                .position(container.width / 3, container.height / 2)
        val thrustParticle = particles["thrust"] ?: error("Could not find thrust particle")
        val thrustSound = resourcesVfs["heavy_steam.wav"].readSound().apply { pitch -= 1.0 }
        val torqueSound = resourcesVfs["heavy_steam.wav"].readSound()
        playerShip = Ship(shipSprite, thrustParticle, thrustSound, torqueSound)
        playerShip.yVel += earth.getOrbitalVelocity(playerShip)
        installShipControls()
        installGravity(playerShip)
        container.addChild(shipSprite)

    }

    private fun installShipControls() {
        val ks = KeyState(stage)
        container.addUpdater {
            if (ks[Key.LEFT] || ks[Key.A]) {
                if (playerShip.torqueSoundChannel.volume < 0.2) playerShip.torqueSoundChannel.volume += 0.1
                playerShip.thrustLeft()
                playerShip.frontRightThrust.emitting = true
                playerShip.backLeftThrust.emitting = true
            } else {
                playerShip.frontRightThrust.emitting = false
                playerShip.backLeftThrust.emitting = false
            }
            if (ks[Key.RIGHT] || ks[Key.D]) {
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

            if (ks[Key.UP] || ks[Key.W]) {
                if (playerShip.thrustSoundChannel.volume < 1.0) playerShip.thrustSoundChannel.volume += 0.1
                playerShip.thrustForward()
                playerShip.forwardThrust.emitting = true
            } else {
                if (playerShip.thrustSoundChannel.volume > 0.0) playerShip.thrustSoundChannel.volume -= 0.1
                playerShip.forwardThrust.emitting = false
            }
        }
        stage.keys {
            up(Key.SPACE) {
                playerShip.fire()
                fireProjectile()
            }
        }
    }

    private fun fireProjectile() {
        // createProjectileObject
        // install collision detection
        // set velocity vector
        // add gravity
        // add offScreenDeath

        val bulletSprite = Sprite(resources["bullet"] ?: error("Could not find bullet resource"))
                .anchor(0.5, 0.5)
                .scale(2.0, 2.0)
                .position(playerShip.sprite.pos)
                .rotation(playerShip.sprite.rotation + Angle.fromDegrees(90))
        val bullet = MassObject(mass = 5.0, sprite = bulletSprite).apply {
            xVel = playerShip.xVel
            yVel = playerShip.yVel
            rVel = playerShip.rVel
        }
        bullet.applyForce(0.5, playerShip.sprite.rotation)
        installGravity(bullet)
        container.addChildAt(bulletSprite, container.getChildIndex(playerShip.sprite) - 1)

        bullet.sprite.addUpdater {
            asteroids.filter { bullet.sprite.collidesWith(it.sprite) }
                    .forEach {
                        it.hit()
                        asteroids -= it
                        bullet.sprite.removeFromParent()
                        bullet.sprite.removeAllComponents()
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
            val gravForce = earth.getGravForce(massObject)
            gravForce.x = gravForce.x * it.milliseconds
            gravForce.y = gravForce.y * it.milliseconds
            massObject.applyForce(gravForce)
        }
    }

    private fun setupAsteroids(numberOfAsteroids: Int) {
        // TODO: Refactor into asteroid field
        val asteroidPos = sequence<Point> {
            while (true) {
                val pos = Point(Random.nextInt(stage.width.toInt()), Random.nextInt(stage.height.toInt()))
                if (pos.distanceTo(earth.sprite.pos) > earth.sprite.width * 1.5 &&
                        pos.distanceTo(earth.sprite.pos) < stage.height / 2) yield(pos)
            }
        }.iterator()
        repeat(numberOfAsteroids) {
            val asteroidSprite = Sprite(resources["asteroid"] ?: error("Could not find asteroid resource"))
                    .position(asteroidPos.next())
                    .anchor(0.5, 0.5)
            val nAsteroid = Asteroid(Random.nextInt(2000..2001).toDouble(), asteroidSprite)

            val orbitAngle = nAsteroid.sprite.pos.angleTo(earth.sprite.pos).plus(Angle.fromDegrees(90))
            val orbitVelocity = earth.getOrbitalVelocity(nAsteroid)
            nAsteroid.xVel = cos(orbitAngle) * orbitVelocity
            nAsteroid.yVel = sin(orbitAngle) * orbitVelocity

            installGravity(nAsteroid)

            container.addChild(nAsteroid.sprite)
            asteroids += nAsteroid

            nAsteroid.sprite.addUpdater {
                if (nAsteroid.sprite.collidesWith(playerShip.sprite)) {
                    destroyPlayer()
                }
            }
        }
    }

    private fun destroyPlayer() {
        onEnd(this)
    }

    private fun setupDisplay() {
        val debug = Debug(container)
        debug.track { "Asteroids remaining: ${asteroids.count()}" }
        debug.track { "Fuel: ${(playerShip.fuel / 30.0).roundToInt()}%" }
    }
}
