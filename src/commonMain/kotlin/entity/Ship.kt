package entity

import MassObject
import com.soywiz.korge.view.Sprite

class Ship(sprite: Sprite) : MassObject(mass = 2000.0, sprite = sprite) {
	private val thrustStrength = 0.1
	private val torqueStrength = 1.0
	val health: Int = 100

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