import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors

suspend fun main() = Korge(width = 2048, height = 1024, bgcolor = Colors.LIGHTGREEN) {
    val game = Game(this)
    game.start()
}

fun createProjectile(source: MassObject, sprite: Sprite): MassObject {
    sprite.position(source.sprite.pos)
    return MassObject(sprite = sprite).apply {
        xVel = source.xVel
        yVel = source.yVel
        rVel = source.rVel
    }
}