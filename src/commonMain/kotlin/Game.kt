import com.soywiz.korev.Key
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import debug.Debug

class Game(val stage: Stage) {
    val resources = mutableMapOf<String, Bitmap>()
    lateinit var playerShip: Ship
    val orbitalObjects = setOf<MassObject>()

    suspend fun start() {
        loadResources()
        setupPlayerShip()
        setupEarth()

        setupDebugLines()
    }

    private suspend fun loadResources() {
        resources += "ship" to resourcesVfs["ship.png"].readBitmap()
        resources += "bullet" to resourcesVfs["bullet.png"].readBitmap()
    }

    private fun setupPlayerShip() {
        val shipSprite = Sprite(resources["ship"] ?: error("Could not find ship resource"))
                .anchor(.5, .5)
                .position(512, 512)
                .scale(3.0, 3.0)
        playerShip = Ship(shipSprite)
        installShipControls()
        stage.addChild(shipSprite)
    }

    private fun installShipControls() {
        val ks = KeyState(stage)
        stage.addUpdater {
            if (ks[Key.LEFT] || ks[Key.A]) playerShip.thrustLeft()
            if (ks[Key.RIGHT] || ks[Key.D]) playerShip.thrustRight()
            if (ks[Key.UP] || ks[Key.W]) playerShip.thrustForward()
            if (ks[Key.SPACE]) {
                playerShip.fire()
            }
        }
    }

    private fun setupEarth() {

    }

    private fun setupDebugLines() {
        val debug = Debug(stage)
        debug.track(playerShip::xVel) { playerShip.xVel }
        debug.track(playerShip::yVel) { playerShip.yVel }
        debug.track(playerShip::rVel) { playerShip.rVel }
        debug.track(playerShip.sprite::rotation) { playerShip.sprite.rotation }
    }
}