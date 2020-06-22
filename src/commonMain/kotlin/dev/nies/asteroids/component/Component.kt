@file:Suppress("OverridingDeprecatedMember")

package dev.nies.asteroids.component

import com.soywiz.korev.Key
import com.soywiz.korge.component.Component
import com.soywiz.korge.component.UpdateComponent
import com.soywiz.korge.component.UpdateComponentWithViews
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
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

class ShipControls(override val view: View) : UpdateComponentWithViews {
    var left: Boolean by view.propDelegate(false)
    var right: Boolean by view.propDelegate(false)
    var forward: Boolean by view.propDelegate(false)
    var shoot: Boolean by view.propDelegate(false)

    override fun update(views: Views, ms: Double) {
        val keys = views.input.keys
        left = keys[Key.LEFT] || keys[Key.A]
        right = keys[Key.RIGHT] || keys[Key.D]
        forward = keys[Key.UP] || keys[Key.W]
        shoot = keys.justPressed(Key.SPACE)
    }
}

fun <T : View> T.withShipControls(): T {
    getOrCreateComponentUpdateWithViews { ShipControls(this) }
    return this
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

fun <T : View> T.withVelocity(xVel: Double, yVel: Double, rVel: Double): T {
    val vel = getOrCreateComponentOther { Velocity(this) }
    vel.xVel = xVel
    vel.yVel = yVel
    vel.rVel = rVel
    return this
}

class Mass(override val view: View, value: Int = 0) : Component {
    var massValue: Int by view.propDelegate(value)
}

fun <T : View> T.withMass(value: Int): T {
    val mass = getOrCreateComponentOther { Mass(this, value) }
    mass.massValue = value
    return this
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
        velocity.xVel += force.x / mass.massValue
        velocity.yVel += force.y / mass.massValue
    }

    fun applyTorque(magnitude: Double) {
        velocity.rVel += magnitude / mass.massValue
    }
}
