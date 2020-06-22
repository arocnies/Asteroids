package dev.nies.asteroids.component

import com.soywiz.korge.component.UpdateComponent
import com.soywiz.korge.component.attach
import com.soywiz.korge.view.View
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.cos
import com.soywiz.korma.geom.sin
import kotlin.math.pow
import kotlin.math.sqrt

class GravityField {
    private val gravityAlgorithm = GravityAlgorithm()
    private val sources = mutableSetOf<Mass>()

    fun View.withSourceGravitation(mass: Int) {
        val massComponent = Mass(this, mass)
        addComponent(massComponent)
        sources += massComponent
    }

    fun View.withReceiverGravitation(mass: Int) {
        val gravComponent = Gravitation(this, mass)
        addComponent(gravComponent)
    }

    @Suppress("DEPRECATION", "OverridingDeprecatedMember")
    inner class Gravitation(override val view: View, mass: Int) : UpdateComponent {
        private val mass = view.getOrCreateComponent { Mass(it, mass) }
        private val force = view.getOrCreateComponent { Force(it) }

        override fun update(ms: Double) = with(gravityAlgorithm) {
            // loop through all sources and apply gravity
            val totalGravForce: Point =
                    sources.asSequence()
                            .map { mass.getGravForce(it) }
                            .reduce { acc, point -> acc + point }
            force.applyForce(totalGravForce)
        }
    }
}

class GravityAlgorithm(private val gravityConstant: Double = 0.00001) {
    fun Mass.getGravForce(other: Mass): Point {
        val distanceToOther: Double = view.pos.distanceTo(other.view.pos)
        val magnitude = gravityConstant * ((this.mass * other.mass) / distanceToOther.pow(2))
        val angleToOther: Angle = view.pos.angleTo(other.view.pos)
        return Point(x = cos(angleToOther) * magnitude, y = sin(angleToOther) * magnitude)
    }

    fun Mass.getCircularVelocity(target: GravityField.Gravitation): Double {
        return sqrt((gravityConstant * mass) / target.view.pos.distanceTo(view.pos))
    }
}
