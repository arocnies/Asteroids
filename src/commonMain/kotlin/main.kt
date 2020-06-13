import com.soywiz.korge.Korge
import com.soywiz.korim.color.Colors

suspend fun main() = Korge(width = 1600, height = 900, bgcolor = Colors["#031e4a"]) {
    val game = Game(this)
    game.start()
}