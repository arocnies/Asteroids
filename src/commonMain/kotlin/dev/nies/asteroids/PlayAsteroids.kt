package dev.nies.asteroids

import com.soywiz.korau.sound.NativeSound
import com.soywiz.korau.sound.readSound
import com.soywiz.korev.Key
import com.soywiz.korge.input.onClick
import com.soywiz.korge.particle.ParticleEmitter
import com.soywiz.korge.particle.readParticle
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Anchor
import dev.nies.asteroids.component.*
import entity.Asteroid
import entity.Earth
import entity.Ship

class PlayAsteroids : Scene() {
    private val gravityField = GravityField()

    // --- New above, old below
    val resources = mutableMapOf<String, Bitmap>()
    val particles = mutableMapOf<String, ParticleEmitter>()
    val sounds = mutableMapOf<String, NativeSound>()

    lateinit var earth: View
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
        text("loading...").centerOn(sceneContainer)
        loadResources()
        addStarField(SolidRect(width = 1.0, height = 1.0, color = Colors.WHITE), 0.01)
        sceneContainer.addChild(gravityField)
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
        earth = Sprite(resources["earth"] ?: error("Could not find earth resource"))
                .anchor(Anchor.MIDDLE_CENTER)
                .position(sceneContainer.width / 2, sceneContainer.height / 2)
                .withMass(100_000)
                .withVelocity(0.01, 2.0, 0.02)
                .addTo(gravityField)
    }

    private fun setupPlayerShip() {
        val shipSprite: Sprite = with(gravityField) {
            Sprite(resources["ship"] ?: error("Could not find ship resource"))
                    .anchor(Anchor.MIDDLE_CENTER)
                    .position(200, 200)
                    .withShipControls()
                    .withGravitation(2000)
                    .withVelocity(0.0, 0.05, 0.0)
        }
        gravityField.addChild(shipSprite)
//
        val thrustParticle = particles["thrust"] ?: error("Could not find thrust particle")
        val thrustSound = sounds["thrust"]!!.apply { pitch -= 1.0 }
        val torqueSound = sounds["torque"]!!
        val shootSound = sounds["shoot"]!!
        playerShip = Ship(shipSprite, thrustParticle, thrustSound, torqueSound, shootSound)

        val controls = shipSprite.getOrCreateComponentUpdateWithViews { ShipControls(it) }
        shipSprite.addUpdater {
            if (controls.left && playerShip.fuel > 0) {
                if (playerShip.torqueSoundChannel.volume < 0.2) playerShip.torqueSoundChannel.volume += 0.1
                playerShip.thrustLeft()
                playerShip.frontRightThrust.emitting = true
                playerShip.backLeftThrust.emitting = true
            } else {
                playerShip.frontRightThrust.emitting = false
                playerShip.backLeftThrust.emitting = false
            }
            if (controls.right && playerShip.fuel > 0) {
                if (playerShip.torqueSoundChannel.volume < 0.2) playerShip.torqueSoundChannel.volume += 0.1
                playerShip.thrustRight()
                playerShip.frontLeftThrust.emitting = true
                playerShip.backRightThrust.emitting = true
            } else {
                playerShip.frontLeftThrust.emitting = false
                playerShip.backRightThrust.emitting = false
            }
            if (!controls.left && !controls.right) {
                if (playerShip.torqueSoundChannel.volume > 0.0) playerShip.torqueSoundChannel.volume -= 0.1
            }
            if (controls.forward && playerShip.fuel > 0) {
                if (playerShip.thrustSoundChannel.volume < 1.0) playerShip.thrustSoundChannel.volume += 0.1
                playerShip.thrustForward()
                playerShip.forwardThrust.emitting = true
            } else {
                if (playerShip.thrustSoundChannel.volume > 0.0) playerShip.thrustSoundChannel.volume -= 0.1
                playerShip.forwardThrust.emitting = false
            }
        }

//        installShipControls()
//        installGravity(playerShip)
//        container.addChild(shipSprite)
//
//        earth.sprite.addUpdater {
//            if (running) {
//                if (playerShip.sprite.pos.distanceTo(earth.sprite.pos) > (stage?.width ?: 0.0) / 2.0 ||
//                        playerShip.sprite.pos.distanceTo(earth.sprite.pos) < earth.sprite.width / 2) {
//                    destroyPlayer()
//                }
//            }
//        }
    }
}
