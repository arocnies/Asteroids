import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors

suspend fun main() = Korge(width = 1024, height = 720, bgcolor = Colors.DARKGRAY) {
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