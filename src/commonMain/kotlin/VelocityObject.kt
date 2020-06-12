import com.soywiz.korge.view.Sprite
import com.soywiz.korge.view.addUpdater

/**
 * An object that changes position and rotation based on it's velocity.
 */
open class VelocityObject(val mass: Double = 0.0, val sprite: Sprite) {
	var xVel: Double = 0.0
	var yVel: Double = 0.0
	var rVel: Double = 0.0

	init {
	    sprite.addUpdater {
			this.rotationDegrees += (rVel * it.milliseconds)
			this.x += (xVel * it.milliseconds)
			this.y += (yVel * it.milliseconds)
		}
	}
}