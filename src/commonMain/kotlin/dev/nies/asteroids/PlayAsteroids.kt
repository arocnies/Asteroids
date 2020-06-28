package dev.nies.asteroids

import com.soywiz.klock.seconds
import com.soywiz.korau.sound.NativeSound
import com.soywiz.korau.sound.readSound
import com.soywiz.korev.Key
import com.soywiz.korge.input.onClick
import com.soywiz.korge.particle.ParticleEmitter
import com.soywiz.korge.particle.readParticle
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.launch
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Point
import dev.nies.asteroids.component.*
import entity.Asteroid
import entity.Earth
import entity.Ship
import kotlin.random.Random

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
        addStarField(SolidRect(width = 1.0, height = 1.0, color = Colors.WHITE), 0.005)
        sceneContainer.addChild(gravityField)
        setupEarth()
        setupPlayerShip()
        setupAsteroids(15)
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
        with(gravityField) {
            val shipSprite = Sprite(resources["ship"]!!).anchor(Anchor.MIDDLE_CENTER)

            val thrustParticle = particles["thrust"]!!
            val thrustSound = sounds["thrust"]!!.apply { pitch -= 1.0 }
            val torqueSound = sounds["torque"]!!
            val shootSound = sounds["shoot"]!!

            playerShip = Ship(shipSprite, thrustParticle, thrustSound, torqueSound, shootSound)
                    .position(200, 200)
                    .withGravitation(mass = 2000)
                    .withVelocity(0.0, 0.05, 0.0)
        }
        sceneContainer.addChild(playerShip)

        playerShip.addUpdater {
            if (
                    playerShip.x < 0 ||
                    playerShip.x > sceneContainer.width ||
                    playerShip.y < 0 ||
                    playerShip.y > sceneContainer.height ||
                    playerShip.pos.distanceTo(earth.pos) < earth.width / 2
            ) launch { playerShip.destroy() }
        }
    }

    private fun setupAsteroids(numOfAsteroids: Int) {
        // TODO: Refactor into asteroid field
        val asteroidPos = sequence<Point> {
            while (true) {
                val pos = Point(Random.nextInt(sceneContainer.width.toInt()), Random.nextInt(sceneContainer.height.toInt()))
                if (pos.distanceTo(earth.pos) > earth.width &&
                        pos.distanceTo(earth.pos) < sceneContainer.width / 2 &&
                        pos.distanceTo(playerShip.pos) > playerShip.width) yield(pos)
            }
        }.iterator()
        repeat(numOfAsteroids) {
            val asteroidSprite = with(gravityField) {
                Sprite(resources["asteroid_${Random.nextInt(3)}"]!!)
                        .position(asteroidPos.next())
                        .anchor(0.5, 0.5)
                        .alpha(0.0)
                        .withGravitation(2000)
                        .withCircularOrbit(earth, 2000)
            }
            launch {
                asteroidSprite.tween(asteroidSprite::alpha[1.0], time = 0.5.seconds)
            }

            val velocity = asteroidSprite.getOrCreateComponentOther { Velocity(it) }
            velocity.rVel = (Random.nextDouble() - Random.nextDouble()) / 4
            sceneContainer.addChild(asteroidSprite)

            asteroidSprite.addUpdater {
                if (asteroidSprite.pos.distanceTo(playerShip.pos) < asteroidSprite.width / 2) {
                    launch { playerShip.destroy() }
                }
            }
        }
    }
}
