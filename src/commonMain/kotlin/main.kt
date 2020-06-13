import com.soywiz.korge.Korge
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import kotlin.random.Random

suspend fun main() = Korge(width = 1600, height = 900, bgcolor = Colors["#010a19"], fullscreen = true) {
    setupStarField(SolidRect(width = 1.0, height = 1.0, color = Colors.WHITE), 0.01)

    val game = Game(this)
    game.start()
}

/**
 * [density] value of 1.0 means a star per pixel randomly placed in the container.
 */
private fun Container.setupStarField(star: View, density: Double) {
    val numberOfStars = (this.width * this.height * density).toInt()
    repeat(numberOfStars) {
        this.addChild(
                star.clone().position(
                        Random.nextInt((width + 1).toInt()),
                        Random.nextInt((height + 1).toInt())
                ).alpha(Random.nextDouble())
        )
    }
}
