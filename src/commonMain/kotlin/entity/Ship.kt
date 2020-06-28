package entity

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.korau.sound.NativeSound
import com.soywiz.korge.particle.ParticleEmitter
import com.soywiz.korge.particle.ParticleEmitterView
import com.soywiz.korge.particle.particleEmitter
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.interpolation.Easing
import dev.nies.asteroids.component.Force
import dev.nies.asteroids.component.ShipControls

class Ship(sprite: Sprite,
           thrustEmitter: ParticleEmitter,
           thrustSound: NativeSound,
           torqueSound: NativeSound,
           private val shootSound: NativeSound) : Container() {
    private val force by lazy { getOrCreateComponentOther { Force(it) } }
    private val controls by lazy { getOrCreateComponentUpdateWithViews { ShipControls(it) } }
    private val startingFuel = 3000.0
    private val thrustStrength = 0.1
    private val torqueStrength = 0.3
    private var fuel: Double = startingFuel
    private val forwardThrust: ParticleEmitterView = sprite.particleEmitter(thrustEmitter)
            .scale(0.3, 0.3)
            .rotation(Angle.fromDegrees(-90))
            .position(sprite.width / -2.0, 0.0)
            .addTo(sprite)
    private val frontLeftThrust: ParticleEmitterView = sprite.particleEmitter(thrustEmitter)
            .scale(0.2, 0.2)
            .position(8.0, sprite.height / -2.0)
            .rotation(Angle.fromDegrees(-30))
            .addTo(sprite)
    private val frontRightThrust: ParticleEmitterView = sprite.particleEmitter(thrustEmitter)
            .scale(0.2, 0.2)
            .position(8.0, sprite.height / 2.0)
            .rotation(Angle.fromDegrees(180 + 30))
            .addTo(sprite)
    private val backLeftThrust: ParticleEmitterView = sprite.particleEmitter(thrustEmitter)
            .scale(0.2, 0.2)
            .position(-5.0, sprite.height / -2.0)
            .rotation(Angle.fromDegrees(30))
            .addTo(sprite)
    private val backRightThrust: ParticleEmitterView = sprite.particleEmitter(thrustEmitter)
            .scale(0.2, 0.2)
            .position(-5.0, sprite.height / 2.0)
            .rotation(Angle.fromDegrees(180 - 30))
            .addTo(sprite)
    private val torqueSoundChannel = torqueSound.playForever(TimeSpan(200.0)).apply {
        volume = 0.0
    }
    private val thrustSoundChannel = thrustSound.playForever().apply {
        volume = 0.0
        pitch = 0.1
    }

    init {
        addChild(sprite)
        setupControls()
    }

    private fun setupControls() {
        addUpdater {
            if (controls.left && fuel > 0) {
                if (torqueSoundChannel.volume < 0.2) torqueSoundChannel.volume += 0.1
                thrustLeft(it.milliseconds)
                frontRightThrust.emitting = true
                backLeftThrust.emitting = true
            } else {
                frontRightThrust.emitting = false
                backLeftThrust.emitting = false
            }
            if (controls.right && fuel > 0) {
                if (torqueSoundChannel.volume < 0.2) torqueSoundChannel.volume += 0.1
                thrustRight(it.milliseconds)
                frontLeftThrust.emitting = true
                backRightThrust.emitting = true
            } else {
                frontLeftThrust.emitting = false
                backRightThrust.emitting = false
            }
            if (!controls.left && !controls.right) {
                if (torqueSoundChannel.volume > 0.0) torqueSoundChannel.volume -= 0.1
            }
            if (controls.forward && fuel > 0) {
                if (thrustSoundChannel.volume < 1.0) thrustSoundChannel.volume += 0.1
                thrustForward(it.milliseconds)
                forwardThrust.emitting = true
            } else {
                if (thrustSoundChannel.volume > 0.0) thrustSoundChannel.volume -= 0.1
                forwardThrust.emitting = false
            }
            if (controls.shoot) {
                fire()
            }
        }
    }

    fun thrustLeft(ms: Double) {
        if (fuel > 0) {
            force.applyTorque(-torqueStrength * ms)
            fuel -= 1 / (torqueStrength * ms)
        }
    }

    fun thrustRight(ms: Double) {
        if (fuel > 0) {
            force.applyTorque(torqueStrength * ms)
            fuel -= 1 / (torqueStrength * ms)
        }
    }

    fun thrustForward(ms: Double) {
        if (fuel > 0) {
            force.applyForce(thrustStrength * ms, angle = rotation)
            fuel -= 1 / (thrustStrength * ms)
        }
    }

    fun fire() {
        shootSound.play()
    }

    suspend fun destroy() {
        shootSound.play()
        thrustSoundChannel.volume = 0.0
        torqueSoundChannel.volume = 0.0
        removeAllComponents()
        forwardThrust.emitting = false
        frontLeftThrust.emitting = false
        frontRightThrust.emitting = false
        backLeftThrust.emitting = false
        backRightThrust.emitting = false
        tween(
                this::alpha[0],
                time = 1.seconds, easing = Easing.EASE_IN_OUT
        )
    }
}