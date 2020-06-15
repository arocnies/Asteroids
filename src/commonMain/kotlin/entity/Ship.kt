package entity

import com.soywiz.klock.TimeSpan
import com.soywiz.korau.sound.NativeSound
import com.soywiz.korge.particle.ParticleEmitter
import com.soywiz.korge.particle.ParticleEmitterView
import com.soywiz.korge.particle.particleEmitter
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.Angle

class Ship(sprite: Sprite, thrustEmitter: ParticleEmitter, thrustSound: NativeSound, torqueSound: NativeSound, val shootSound: NativeSound) : MassObject(mass = 2000.0, sprite = sprite) {
	val startingFuel = 3000.0
    var fuel: Double = startingFuel
    private val thrustStrength = 0.8
	private val torqueStrength = 5.0

	val forwardThrust: ParticleEmitterView
	val frontLeftThrust: ParticleEmitterView
	val frontRightThrust: ParticleEmitterView
	val backLeftThrust: ParticleEmitterView
	val backRightThrust: ParticleEmitterView
	val torqueSoundChannel = torqueSound.playForever(TimeSpan(200.0)).apply {
		volume = 0.0
	}
	val thrustSoundChannel = thrustSound.playForever().apply {
		volume = 0.0
		pitch = 0.1
	}

	init {
		forwardThrust = sprite.particleEmitter(thrustEmitter)
				.scale(0.3, 0.3)
				.rotation(Angle.fromDegrees(-90))
				.position(sprite.width / -2.0, 0.0)
		sprite.addChild(forwardThrust)

		frontLeftThrust = sprite.particleEmitter(thrustEmitter)
				.scale(0.2, 0.2)
				.position(8.0, sprite.height / -2.0)
				.rotation(Angle.fromDegrees(-30))
		sprite.addChild(frontLeftThrust)

		frontRightThrust = sprite.particleEmitter(thrustEmitter)
				.scale(0.2, 0.2)
				.position(8.0, sprite.height / 2.0)
				.rotation(Angle.fromDegrees(180 + 30))
		sprite.addChild(frontRightThrust)

		backLeftThrust = sprite.particleEmitter(thrustEmitter)
				.scale(0.2, 0.2)
				.position(-5.0, sprite.height / -2.0)
				.rotation(Angle.fromDegrees(30))
		sprite.addChild(backLeftThrust)

		backRightThrust = sprite.particleEmitter(thrustEmitter)
				.scale(0.2, 0.2)
				.position(-5.0, sprite.height / 2.0)
				.rotation(Angle.fromDegrees(180 - 30))
		sprite.addChild(backRightThrust)
	}

	fun thrustLeft() {
		if (fuel > 0) applyTorque(-torqueStrength)
		fuel -= 1 / torqueStrength
	}
	fun thrustRight() {
		if (fuel > 0) applyTorque(torqueStrength)
		fuel -= 1 / torqueStrength
	}
	fun thrustForward() {
		if (fuel > 0) applyForce(thrustStrength, angle = sprite.rotation)
		fuel -= 1 / thrustStrength
	}
	fun fire() {
		println("FIRE!")
		shootSound.play()
	}
}