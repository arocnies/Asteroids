package dev.nies.asteroids

import com.soywiz.korev.Key
import com.soywiz.korge.input.keys
import com.soywiz.korge.input.onClick
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.centerOn
import com.soywiz.korge.view.container
import com.soywiz.korim.color.Colors
import dev.nies.asteroids.graphics.TextLines

class StartMenu : Scene() {
    override suspend fun Container.sceneInit() {
        addStarField(SolidRect(width = 1.0, height = 1.0, color = Colors.WHITE), 0.01)

        val textLines = TextLines(container())
        textLines.textLine("Asteroids++")
        textLines.textLine("")
        textLines.container.centerOn(this)

        val startButton = textLines.textLine("[ENTER]")
        startButton.keys {
            down(Key.ENTER) {
                sceneContainer.changeTo<PlayAsteroids>()
            }
        }
        startButton.onClick {
            sceneContainer.changeTo<PlayAsteroids>()
        }
    }
}
