package entity

import com.soywiz.korge.particle.ParticleEmitterView
import com.soywiz.korge.view.Sprite
import com.soywiz.korge.view.position

class Ship(sprite: Sprite, val thrust: ParticleEmitterView) : MassObject(mass = 2000.0, sprite = sprite) {
	private val thrustStrength = 0.1
	private val torqueStrength = 1.0
	val health: Int = 100

	init {
		thrust.position(sprite.width / -2.0, 0.0).emitting = false
		sprite.addChild(thrust)
	}

	fun thrustLeft() {
		applyTorque(-torqueStrength)
	}
	fun thrustRight() {
		applyTorque(torqueStrength)
	}
	fun thrustForward() {
		applyForce(thrustStrength, angle = sprite.rotation)
	}
	fun fire() {
		println("FIRE!")
	}
}