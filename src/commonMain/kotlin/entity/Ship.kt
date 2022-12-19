package entity

import com.soywiz.klock.TimeSpan
import com.soywiz.korau.sound.Sound
import com.soywiz.korau.sound.SoundChannel
import com.soywiz.korge.particle.ParticleEmitter
import com.soywiz.korge.particle.ParticleEmitterView
import com.soywiz.korge.particle.particleEmitter
import com.soywiz.korge.view.*
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
	private lateinit var torqueSoundChannel: SoundChannel
	private lateinit var thrustSoundChannel: SoundChannel

	init {
		GlobalScope.launch(Dispatchers.Default) {
			delay(1000)
			torqueSoundChannel = torqueSound.playForever(TimeSpan(200.0)).apply {
				volume = 0.0
			}
			views().stage.addUpdater {
				torqueSoundChannel.volume = torqueVolume
			}
		}
		GlobalScope.launch(Dispatchers.Default) {
			delay(1000)
			thrustSoundChannel = thrustSound.playForever().apply {
				volume = 0.0
				pitch = 0.1
			}
			views().stage.addUpdater {
				thrustSoundChannel.volume = thrustVolume
			}
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
		GlobalScope.launch {
			shootSound.play()
		}
	}
}