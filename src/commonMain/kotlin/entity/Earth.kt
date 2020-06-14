package entity

import com.soywiz.korge.view.Sprite
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.cos
import com.soywiz.korma.geom.sin
import kotlin.math.pow
import kotlin.math.sqrt

const val gravityConstant = 0.00001

class Earth(val sprite: Sprite) {
    val mass = 100_000
    val health = 100

    fun getGravForce(massObject: MassObject): Point {
        val distanceToObject: Double = sprite.pos.distanceTo(massObject.sprite.pos)
        val magnitude = gravityConstant * ((massObject.mass * mass) / distanceToObject.pow(2))
        val angleObjectToEarth: Angle = massObject.sprite.pos.angleTo(sprite.pos)
        return Point(x = cos(angleObjectToEarth) * magnitude, y = sin(angleObjectToEarth) * magnitude)
    }

    fun getOrbitalVelocity(massObject: MassObject): Double {
        return sqrt((gravityConstant * mass) / massObject.sprite.pos.distanceTo(sprite.pos))
    }
}