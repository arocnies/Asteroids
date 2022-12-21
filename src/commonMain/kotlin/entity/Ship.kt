package entity

import com.soywiz.klock.TimeSpan
import com.soywiz.korau.sound.PlaybackTimes
import com.soywiz.korau.sound.Sound
import com.soywiz.korau.sound.SoundChannel
import com.soywiz.korge.particle.ParticleEmitter
import com.soywiz.korge.particle.ParticleEmitterView
import com.soywiz.korge.particle.particleEmitter
import com.soywiz.korge.view.*
import com.soywiz.korio.async.runBlockingNoSuspensions
import com.soywiz.korma.geom.Angle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Ship(sprite: Container, thrustEmitter: ParticleEmitter, thrustSound: Sound, torqueSound: Sound, val shootSound: Sound) : MassObject(mass = 2000.0, sprite = sprite) {
	val startingFuel = 3000.0
    var fuel: Double = startingFuel
    private val thrustStrength = 0.8
	private val torqueStrength = 5.0

	val forwardThrust: ParticleEmitterView
	val frontLeftThrust: ParticleEmitterView
	val frontRightThrust: ParticleEmitterView
	val backLeftThrust: ParticleEmitterView
	val backRightThrust: ParticleEmitterView
	var torqueVolume: Double = 0.0
	var thrustVolume: Double = 0.0
	private var torqueSoundChannel: SoundChannel? = null
	private var thrustSoundChannel: SoundChannel? = null

	init {
		torqueSoundChannel = torqueSound.playNoCancelForever().apply {
			volume = 0.0
		}
		sprite.addUpdater {
			torqueSoundChannel?.volume = torqueVolume
		}
		thrustSoundChannel = thrustSound.playNoCancelForever().apply {
			volume = 0.0
			pitch = 0.1
		}
		sprite.addUpdater {
			thrustSoundChannel?.volume = thrustVolume
		}

		forwardThrust = ParticleEmitterView(thrustEmitter)
				.scale(0.3, 0.3)
				.rotation(Angle.fromDegrees(-90))
				.position(sprite.width / -2.0, 0.0)
		sprite.addChild(forwardThrust)

		frontLeftThrust = ParticleEmitterView(thrustEmitter)
				.scale(0.2, 0.2)
				.position(8.0, sprite.height / -2.0)
				.rotation(Angle.fromDegrees(-30))
		sprite.addChild(frontLeftThrust)

		frontRightThrust = ParticleEmitterView(thrustEmitter)
				.scale(0.2, 0.2)
				.position(8.0, (sprite.height / 2.0) - 3)
				.rotation(Angle.fromDegrees(180 + 30))
		sprite.addChild(frontRightThrust)

		backLeftThrust = ParticleEmitterView(thrustEmitter)
				.scale(0.2, 0.2)
				.position(-5.0,  sprite.height / -4.0)
				.rotation(Angle.fromDegrees(30))
		sprite.addChild(backLeftThrust)

		backRightThrust = ParticleEmitterView(thrustEmitter)
				.scale(0.2, 0.2)
				.position(-5.0, sprite.height / 4.0)
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
		shootSound.playNoCancel(PlaybackTimes.ONE)
	}
}