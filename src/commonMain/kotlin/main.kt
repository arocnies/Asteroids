import com.soywiz.korev.Key
import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.cos
import com.soywiz.korma.geom.sin
import debug.Debug

suspend fun main() = Korge(width = 2048, height = 1024, bgcolor = Colors.LIGHTGREEN) {
	val debug = Debug(this)
	val shipSprit = Sprite(resourcesVfs["ship.png"].readBitmap()).anchor(.5, .5).position(512, 512).scale(3.0, 3.0)
	val ship = Ship(shipSprit)
	this.addChild(ship.sprite)

	debug.track(ship::xVel) { ship.xVel }
	debug.track(ship::yVel) { ship.yVel }
	debug.track(ship::rVel) { ship.rVel }
	debug.track(ship.sprite::rotation) { ship.sprite.rotation }

	val ks = KeyState(this)
	addUpdater {
		if (ks[Key.LEFT] || ks[Key.A]) ship.thrustLeft()
		if (ks[Key.RIGHT] || ks[Key.D]) ship.thrustRight()
		if (ks[Key.UP] || ks[Key.W]) ship.thrustForward()
	}
}

class Ship(sprite: Sprite) : VelocityObject(sprite = sprite) {
	private val thrustMultiplier = 0.001

	fun thrustLeft() {
		rVel -= thrustMultiplier
	}
	fun thrustRight() {
		rVel += thrustMultiplier
	}
	fun thrustForward() {
		xVel += sin(sprite.rotation) * thrustMultiplier
		yVel += -cos(sprite.rotation) * thrustMultiplier
	}
}
