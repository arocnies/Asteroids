import com.soywiz.korge.view.Sprite
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.collidesWithGlobalBoundingBox
import com.soywiz.korma.geom.Point

class ScreenArrowRegister(screenView: View) {
    private val trackedSprites = mutableSetOf<Sprite>()

    init {
        screenView.addUpdater {
            trackedSprites.filter { !it.collidesWithGlobalBoundingBox(screenView) }
                    .forEach {
                        val globalCenterOfScreenView = localToGlobal(screenView.pos)
                                .plus(Point(screenView.width / 2, screenView.y / 2))
                        val globalSpritePos = localToGlobal(it.pos)
                        val angleCenterScreenToSprite = globalCenterOfScreenView.angleTo(globalSpritePos)

                    }
        }
    }

    fun track(sprite: Sprite) {
        trackedSprites += sprite
    }

    fun untrack(sprite: Sprite) {
        trackedSprites -= sprite
    }
}