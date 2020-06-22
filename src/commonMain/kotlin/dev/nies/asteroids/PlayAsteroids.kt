package dev.nies.asteroids

import com.soywiz.korau.sound.NativeSound
import com.soywiz.korau.sound.readSound
import com.soywiz.korge.particle.ParticleEmitter
import com.soywiz.korge.particle.readParticle
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Anchor
import dev.nies.asteroids.component.ShipControls
import entity.Asteroid
import entity.Earth
import entity.Ship

class PlayAsteroids : Scene() {
    val resources = mutableMapOf<String, Bitmap>()
    val particles = mutableMapOf<String, ParticleEmitter>()
    val sounds = mutableMapOf<String, NativeSound>()

    lateinit var earth: Earth
    lateinit var playerShip: Ship
    var ammo = 10
    var asteroidsKilled = 0
    var tanksCollected = mutableSetOf<Asteroid>()

    private var nextWaveCountdown: Int = 20
    var wave = 1
    var running = true
    val score
        get() = ((wave - 1) * 100) + (asteroidsKilled * 50) + (tanksCollected.size * 100)

    override suspend fun Container.sceneInit() {
        addStarField(SolidRect(width = 1.0, height = 1.0, color = Colors.WHITE), 0.01)
        loadResources()
        setupEarth()
        setupPlayerShip()
//        setupAsteroids(15)
//        setupDisplay()
    }

    private suspend fun loadResources() {
        resources += "ship" to resourcesVfs["ship_2.png"].readBitmap()
        resources += "bullet" to resourcesVfs["bullet.png"].readBitmap()
        resources += "earth" to resourcesVfs["earth.png"].readBitmap()
        resources += "asteroid_0" to resourcesVfs["asteroid_0.png"].readBitmap()
        resources += "asteroid_1" to resourcesVfs["asteroid_1.png"].readBitmap()
        resources += "asteroid_2" to resourcesVfs["asteroid_2.png"].readBitmap()
        resources += "tank" to resourcesVfs["tank.png"].readBitmap()
        particles += "thrust" to resourcesVfs["particle/thurst/particle.pex"].readParticle()
        sounds += "thrust" to resourcesVfs["heavy_steam.wav"].readSound()
        sounds += "torque" to resourcesVfs["heavy_steam.wav"].readSound()
        sounds += "shoot" to resourcesVfs["shoot.wav"].readSound()
        sounds += "breaking" to resourcesVfs["breaking.wav"].readSound()
        sounds += "crash" to resourcesVfs["crash.wav"].readSound()
        sounds += "tank" to resourcesVfs["tank.wav"].readSound()
    }

    private fun setupEarth() {
        val earthSprite = Sprite(resources["earth"] ?: error("Could not find earth resource"))
                .anchor(Anchor.MIDDLE_CENTER)
                .position(sceneContainer.width / 2, sceneContainer.height / 2)
        earth = Earth(earthSprite)
        sceneContainer.addChild(earthSprite)
    }

    private suspend fun setupPlayerShip() {
        val shipSprite = Sprite(resources["ship"] ?: error("Could not find ship resource"))
                .anchor(Anchor.MIDDLE_CENTER)
                .position(earth.sprite.pos.x - earth.sprite.width * 2, earth.sprite.pos.y)

        val thrustParticle = particles["thrust"] ?: error("Could not find thrust particle")
        val thrustSound = sounds["thrust"]!!.apply { pitch -= 1.0 }
        val torqueSound = sounds["torque"]!!
        val shootSound = sounds["shoot"]!!

        playerShip = Ship(shipSprite, thrustParticle, thrustSound, torqueSound, shootSound)
        playerShip.yVel += earth.getOrbitalVelocity(playerShip)
        playerShip.rVel = -0.02

        playerShip.sprite.addComponent(ShipControls())

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
}
