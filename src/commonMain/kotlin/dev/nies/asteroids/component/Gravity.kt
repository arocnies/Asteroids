package dev.nies.asteroids.component

import com.soywiz.korge.component.UpdateComponent
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addTo
import com.soywiz.korma.geom.*
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * All children to this container are used as sources of gravity.
 * Any view given a [Gravitation] component produced from this container with [withGravitation] will be
 * effected by the gravity.
 */
class GravityField : Container() {
    private val gravityAlgorithm = GravityAlgorithm()

    fun <T : View> T.withGravitation(mass: Int) : T {
        val gravComponent = Gravitation(this, mass)
        addComponent(gravComponent)
        return this
    }

    fun View.withCircularOrbit(target: View) {
        val gravitation = getOrCreateComponentOther { Gravitation(this) }
        val targetGravitation = target.addTo(this@GravityField).getOrCreateComponentOther { Gravitation(target) }
        if (gravitation.mass.massValue != 0 && targetGravitation.mass.massValue != 0) {
            with(gravityAlgorithm) {
                gravitation.force.applyForce(gravitation.mass.getCircularVelocity(target = targetGravitation))
            }
        }
    }

    @Suppress("DEPRECATION", "OverridingDeprecatedMember")
    inner class Gravitation(override val view: View, mass: Int = 0) : UpdateComponent {
        val mass = view.getOrCreateComponent { Mass(it, mass) }
        val force = view.getOrCreateComponent { Force(it) }

        override fun update(ms: Double) = with(gravityAlgorithm) {
            // loop through all sources and apply gravity
            val totalGravForce: Point = this@GravityField.children
                    .asSequence()
                    .filterNot { it == view }
                    .map { childView -> childView.getOrCreateComponentOther { Mass(it) } }
                    .filterNot { it.massValue == 0 }
                    .map { this@Gravitation.mass.getGravForce(it) }
                    .reduce { acc, point -> acc + point }
            force.applyForce(totalGravForce * ms)
        }
    }

    inner class GravityAlgorithm(private val gravityConstant: Double = 0.0001) {

        fun Mass.getGravForce(other: Mass): Point {
            val distanceToOther: Double = view.pos.distanceTo(other.view.pos) + 1.0
            val magnitude = gravityConstant * ((this.massValue * other.massValue) / distanceToOther.pow(2))
            val angleToOther: Angle = view.globalPos().angleTo(other.view.globalPos())
            return vectorToPoint(magnitude, angleToOther)
        }

        fun Mass.getCircularVelocity(target: GravityField.Gravitation): Point {
            val magnitude = sqrt((gravityConstant * massValue) / target.view.globalPos().distanceTo(view.globalPos()))
            val angleToTarget = target.view.globalPos().angleTo(view.globalPos())
            val orbitAngle = angleToTarget.plus(Angle.fromDegrees(-90)) // FIXME: Test with tangent
            return vectorToPoint(magnitude, orbitAngle)
        }
    }
}

private fun View.globalPos(): Point = this.localToGlobal(this.pos)

private fun vectorToPoint(magnitude: Double, angle: Angle) = Point(
        x = cos(angle) * magnitude,
        y = sin(angle) * magnitude
)
