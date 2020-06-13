package entity

import com.soywiz.korge.view.Sprite
import com.soywiz.korge.view.addUpdater
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.cos
import com.soywiz.korma.geom.sin

/**
 * An object that changes position and rotation based on it's velocity.
 */
open class MassObject(val mass: Double = 1.0, val sprite: Sprite) {
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

    fun applyForce(magnitude: Double, angle: Angle) {
        xVel += (magnitude * cos(angle)) / mass
        yVel += (magnitude * sin(angle)) / mass
    }

    fun applyTorque(magnitude: Double) {
        rVel += magnitude / mass
    }
}