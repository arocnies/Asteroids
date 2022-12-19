package entity

import com.soywiz.korge.view.Sprite
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addUpdater
import com.soywiz.korma.geom.*

/**
 * An object that changes position and rotation based on its velocity.
 */
open class MassObject(val mass: Double = 1.0, val sprite: View) {
    var xVel: Double = 0.0
    var yVel: Double = 0.0
    var rVel: Double = 0.0

    init {
        sprite.addUpdater {
            this.rotation += (rVel * it.milliseconds).degrees
            this.x += (xVel * it.milliseconds)
            this.y += (yVel * it.milliseconds)
        }
    }

    fun applyForce(magnitude: Double, angle: Angle) {
        val x = magnitude * cos(angle)
        val y = magnitude * sin(angle)
        applyForce(Point(x, y))
    }

    fun applyForce(force: Point) {
        xVel += force.x / mass
        yVel += force.y / mass
    }

    fun applyTorque(magnitude: Double) {
        rVel += magnitude / mass
    }
}