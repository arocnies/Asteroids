package dev.nies.asteroids

import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Size
import kotlin.random.Random

fun Container.addStarField(star: View, density: Double) {
    stage?.let {
        solidRect(width = it.unscaledWidth, height = it.unscaledHeight, color = Colors["#010a19"])
        addChild(
                createStarField(
                        star = star,
                        size = Size(it.unscaledWidth, it.unscaledHeight),
                        density = density
                )
        )
    }
}

fun createStarField(star: View, size: Size, density: Double): View {
    return Container().apply {
        addChild(star.xy(100, 100))
        val numberOfStars = (size.width * size.height * density).toInt()
        repeat(numberOfStars) {
            this.addChild(
                    star.clone().position(
                            Random.nextInt((size.width + 1).toInt()),
                            Random.nextInt((size.height + 1).toInt())
                    ).alpha(Random.nextDouble() + 1.0)
            )
        }
    }
}