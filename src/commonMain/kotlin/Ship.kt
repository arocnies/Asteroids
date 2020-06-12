import com.soywiz.korge.view.Sprite
import com.soywiz.korma.geom.cos
import com.soywiz.korma.geom.sin

class Ship(sprite: Sprite) : MassObject(mass = 200.0, sprite = sprite) {
	private val thrustStrength = 1.0
	val health: Int = 100

	fun thrustLeft() {
		applyTorque(-thrustStrength)
	}
	fun thrustRight() {
		applyTorque(thrustStrength)
	}
	fun thrustForward() {
		applyForce(thrustStrength, angle = sprite.rotation)
	}
	fun fire() {
		println("FIRE!")
	}
}