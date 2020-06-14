import com.soywiz.korev.Key
import com.soywiz.korev.keys
import com.soywiz.korge.particle.ParticleEmitter
import com.soywiz.korge.particle.particleEmitter
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt

class Game(val stage: Stage, val container: Container) {
    val resources = mutableMapOf<String, Bitmap>()
    val particles = mutableMapOf<String, ParticleEmitter>()
    val asteroids = mutableSetOf<Asteroid>()
    lateinit var playerShip: Ship
    lateinit var earth: Earth

    suspend fun start() {
        loadResources()
        setupEarth()
        setupPlayerShip()
        setupAsteroids(5)
        setupDisplay()
    }

    private suspend fun loadResources() {
        resources += "ship" to resourcesVfs["ship_2.png"].readBitmap()
        resources += "bullet" to resourcesVfs["bullet.png"].readBitmap()
        resources += "earth" to resourcesVfs["earth.png"].readBitmap()
        resources += "asteroid" to resourcesVfs["asteroid_0.png"].readBitmap()
        particles += "thrust" to resourcesVfs["particle/thurst/particle.pex"].readParticle()
    }

    private fun setupPlayerShip() {
        val shipSprite = Sprite(resources["ship"] ?: error("Could not find ship resource"))
                .anchor(.5, .5)
                .position(container.width / 3, container.height / 2)
        val thrustParticle = container.particleEmitter(particles["thrust"] ?: error("Could not find thrust particle"))
                .scale(0.3, 0.3)
                .rotation(Angle.fromDegrees(-90))
        playerShip = Ship(shipSprite, thrustParticle)
        playerShip.yVel += earth.getOrbitalVelocity(playerShip)
        installShipControls()
        installGravity(playerShip)
        container.addChild(shipSprite)
    }

    private fun installShipControls() {
        val ks = KeyState(stage)
        container.addUpdater {
            if (ks[Key.LEFT] || ks[Key.A]) playerShip.thrustLeft()
            if (ks[Key.RIGHT] || ks[Key.D]) playerShip.thrustRight()
            if (ks[Key.UP] || ks[Key.W]) {
                playerShip.thrustForward()
                playerShip.thrust.emitting = true
            } else {
                playerShip.thrust.emitting = false
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
                .position(playerShip.sprite.pos)
                .rotation(playerShip.sprite.rotation)
        val bullet = MassObject(mass = 5.0, sprite = bulletSprite).apply {
            xVel = playerShip.xVel
            yVel = playerShip.yVel
            rVel = playerShip.rVel
        }
        bullet.applyForce(0.1, playerShip.sprite.rotation)
        installGravity(bullet)
        container.addChildAt(bulletSprite, container.getChildIndex(playerShip.sprite) - 1)

        container.addUpdater {
            asteroids.filter { bullet.sprite.collidesWith(it.sprite) }
                    .forEach {
                        it.hit()
                        asteroids -= it
                        bullet.sprite.removeFromParent()
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
        }
    }

    private fun setupDisplay() {
        val debug = Debug(container)
        debug.track { "Asteroids remaining: ${asteroids.count()}" }
        debug.track { "Fuel: ${(playerShip.fuel / 30.0).roundToInt()}%" }
    }
}
