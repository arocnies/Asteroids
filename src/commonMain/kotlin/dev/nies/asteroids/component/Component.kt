@file:Suppress("OverridingDeprecatedMember")

package dev.nies.asteroids.component

import com.soywiz.korev.Key
import com.soywiz.korge.component.Component
import com.soywiz.korge.component.UpdateComponent
import com.soywiz.korge.view.View
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.cos
import com.soywiz.korma.geom.sin
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <R, T> View.propDelegate(default: T): ReadWriteProperty<R, T> {
    return object : ReadWriteProperty<R, T> {
        override fun getValue(thisRef: R, property: KProperty<*>): T {
            @Suppress("UNCHECKED_CAST")
            return this@propDelegate.props[property.name] as? T ?: default
        }

        override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
            this@propDelegate.addProp(property.name, value)
        }
    }
}

class ShipControls(override val view: View) : UpdateComponent {
    var left: Boolean by view.propDelegate(false)
    var right: Boolean by view.propDelegate(false)
    var forward: Boolean by view.propDelegate(false)
    var shoot: Boolean by view.propDelegate(false)

    override fun update(ms: Double) {
        val keys = view.stage?.views?.input?.keys
        checkNotNull(keys)
        // Takes in input and sets props on view
        left = keys[Key.LEFT] || keys[Key.A]
        right = keys[Key.RIGHT] || keys[Key.D]
        forward = keys[Key.UP] || keys[Key.W]
        shoot = keys.justPressed(Key.SPACE)
    }
}

class Velocity(override val view: View) : UpdateComponent {
    var xVel: Double by view.propDelegate(0.0)
    var yVel: Double by view.propDelegate(0.0)

    var rVel: Double by view.propDelegate(0.0)
    override fun update(ms: Double) {
        view.x += xVel * ms
        view.y += yVel * ms
        view.rotationDegrees += rVel * ms
    }

}

class Mass(override val view: View, value: Int = 0) : Component {
    val mass: Int by view.propDelegate(value)
}

class Force(override val view: View) : Component {
    @Suppress("DEPRECATION")
    val mass = view.getOrCreateComponent { Mass(it) }
    val velocity = view.getOrCreateComponentUpdate { Velocity(it) }

    fun applyForce(magnitude: Double, angle: Angle) {
        val x = magnitude * cos(angle)
        val y = magnitude * sin(angle)
        applyForce(Point(x, y))
    }

    fun applyForce(force: Point) {
        velocity.xVel += force.x / mass.mass
        velocity.yVel += force.y / mass.mass
    }

    fun applyTorque(magnitude: Double) {
        velocity.rVel += magnitude / mass.mass
    }
}
