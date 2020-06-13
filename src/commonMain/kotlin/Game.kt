import com.soywiz.korev.Key
import com.soywiz.korge.particle.ParticleEmitter
import com.soywiz.korge.particle.particleEmitter
import com.soywiz.korge.particle.readParticle
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.Point
import debug.Debug
import entity.Earth
import entity.MassObject
import entity.Ship

class Game(val stage: Stage) {
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
        //setupDebugLines()
    }

    private suspend fun loadResources() {
        resources += "ship" to resourcesVfs["ship_2.png"].readBitmap()
        resources += "bullet" to resourcesVfs["bullet.png"].readBitmap()
        resources += "earth" to resourcesVfs["earth.png"].readBitmap()
        particles += "thrust" to resourcesVfs["particle/thurst/particle.pex"].readParticle()
    }

    private fun setupPlayerShip() {
        val shipSprite = Sprite(resources["ship"] ?: error("Could not find ship resource"))
                .anchor(.5, .5)
                .position(stage.width / 3, stage.height / 2)
        val thrustParticle = stage.particleEmitter(particles["thrust"] ?: error("Could not find thrust particle"))
                .scale(0.3, 0.3)
                .rotation(Angle.fromDegrees(-90))
        playerShip = Ship(shipSprite, thrustParticle)
        playerShip.yVel += earth.getOrbitalVelocityOfDistance(playerShip.sprite.pos.distanceTo(Point(stage.x / 2, stage.y / 2)))
        installShipControls()
        stage.addChild(shipSprite)
    }

    private fun installShipControls() {
        val ks = KeyState(stage)
        stage.addUpdater {
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
                .position(stage.width / 2, stage.height / 2)
        earth = Earth(earthSprite)
        stage.addChild(earthSprite)
    }

    private fun installGravity() {
        stage.addUpdater {
            val gravForce = earth.getGravForce(playerShip)
            gravForce.x = gravForce.x * it.milliseconds
            gravForce.y = gravForce.y * it.milliseconds
            playerShip.applyForce(gravForce)
        }
    }

    private fun setupDebugLines() {
        val debug = Debug(stage)
        debug.track(playerShip::xVel) { playerShip.xVel }
        debug.track(playerShip::yVel) { playerShip.yVel }
        debug.track(playerShip::rVel) { playerShip.rVel }
        debug.track(playerShip.sprite::rotation) { playerShip.sprite.rotation }
    }
}