import com.soywiz.korev.Key
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
import kotlin.random.Random
import kotlin.random.nextInt

class Game(val stage: Stage, val container: Container) {
    val resources = mutableMapOf<String, Bitmap>()
    val particles = mutableMapOf<String, ParticleEmitter>()
    lateinit var playerShip: Ship
    lateinit var earth: Earth
    val orbitalObjects = setOf<MassObject>()

    suspend fun start() {
        loadResources()
        setupEarth()
        setupPlayerShip()
        installGravity()
        setupAsteroids(5)
        //setupDebugLines()
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
            if (ks[Key.SPACE]) {
                playerShip.fire()
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

    private fun installGravity() {
        container.addUpdater {
            val gravForce = earth.getGravForce(playerShip)
            gravForce.x = gravForce.x * it.milliseconds
            gravForce.y = gravForce.y * it.milliseconds
            playerShip.applyForce(gravForce)
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

            container.addUpdater {
                val gravForce = earth.getGravForce(nAsteroid)
                gravForce.x = gravForce.x * it.milliseconds
                gravForce.y = gravForce.y * it.milliseconds
                nAsteroid.applyForce(gravForce)
            }

            container.addChild(nAsteroid.sprite)
        }
    }

    private fun setupDebugLines() {
        val debug = Debug(container)
        debug.track(playerShip::xVel) { playerShip.xVel }
        debug.track(playerShip::yVel) { playerShip.yVel }
        debug.track(playerShip::rVel) { playerShip.rVel }
        debug.track(playerShip.sprite::rotation) { playerShip.sprite.rotation }
    }
}
