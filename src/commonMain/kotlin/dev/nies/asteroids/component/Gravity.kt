package dev.nies.asteroids.component

import com.soywiz.korge.component.UpdateComponent
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.hasAncestor
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

    fun <T : View> T.withGravitation(mass: Int): T {
        val gravComponent = Gravitation(this, mass)
        addComponent(gravComponent)
        return this
    }

    fun <T : View> T.withCircularOrbit(target: View, mass: Int): T {
        require(target.hasAncestor(this@GravityField)) { "Cannot orbit around target not in the same gravity field" }

        val gravitation = getOrCreateComponentOther { Gravitation(this, mass) }
        val targetMass = target.getOrCreateComponentOther { Mass(it) }
        with(gravityAlgorithm) {
            gravitation.force.applyForce(gravitation.mass.getCircularVelocity(target = targetMass))
        }
        return this
    }

    inner class Gravitation(override val view: View, mass: Int) : UpdateComponent {
        val mass = view.getOrCreateComponentOther { Mass(it, mass) }
        val force = view.getOrCreateComponentOther { Force(it) }

        override fun update(ms: Double) = with(gravityAlgorithm) {
            // loop through all sources and apply gravity
            val totalGravForce: Point = Point()
            this@GravityField.forEachChildren { childView ->
                if (view != childView) {
                    val childMass = childView.getOrCreateComponentOther { Mass(it) }
                    val childGrav = this@Gravitation.mass.getGravForce(childMass)
                    totalGravForce += childGrav
                }
            }
            // Alternative for not using "view.forEachChildren"
            // Switched to test performance difference since "view.children" is said to be slow but not sure why.
//            this@GravityField.children
//                    .asSequence()
//                    .filterNot { view == it }
//                    .map { childView -> childView.getOrCreateComponentOther { Mass(it) } }
//                    .filterNot { it.massValue == 0 }
//                    .map { this@Gravitation.mass.getGravForce(it) }
//                    .reduce { acc, point -> acc + point }
            force.applyForce(totalGravForce * ms)
        }
    }
}

private class GravityAlgorithm(private val gravityConstant: Double = 0.00001) {

    fun Mass.getGravForce(other: Mass): Point {
        val distanceToOther: Double = view.pos.distanceTo(other.view.pos)
        val magnitude = gravityConstant * ((this.massValue * other.massValue) / distanceToOther.pow(2))
        val angleToOther: Angle = view.pos.angleTo(other.view.pos)
        return vectorToPoint(magnitude, angleToOther)
    }

    fun Mass.getCircularVelocity(target: Mass): Point {
        val magnitude = sqrt((gravityConstant * massValue) / target.view.globalPos().distanceTo(view.globalPos()))
        val angleToTarget = target.view.globalPos().angleTo(view.globalPos())
        val orbitAngle = angleToTarget.plus(Angle.fromDegrees(-90)) // FIXME: Test with tangent
        return vectorToPoint(magnitude, orbitAngle)
    }
}

private fun View.globalPos(): Point = this.localToGlobal(this.pos)

private fun vectorToPoint(magnitude: Double, angle: Angle) = Point(
        x = cos(angle) * magnitude,
        y = sin(angle) * magnitude
)
