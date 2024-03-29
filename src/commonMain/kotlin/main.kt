import com.soywiz.klock.TimeSpan
import com.soywiz.korau.sound.readMusic
import com.soywiz.korau.sound.readSound
import com.soywiz.korev.Key
import com.soywiz.korge.Korge
import com.soywiz.korge.input.keys
import com.soywiz.korge.input.onClick
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korio.file.std.resourcesVfs
import debug.Debug
import kotlin.random.Random

suspend fun main() = Korge(virtualWidth = 1600, virtualHeight = 900, bgcolor = Colors["#010a19"], fullscreen = false, title = "Asteroids++") {
    val backgroundMusic = resourcesVfs["space-ambient.wav"].readSound()
    val channel = backgroundMusic.playForever()
    showStartMenu(this)
}

private fun showStartMenu(stage: Stage) {
    stage.setupStarField(SolidRect(width = 1.0, height = 1.0, color = Colors.WHITE), 0.01)
    val screenView = Container()
    val debug = Debug(screenView)
    val title = debug.textLine("Asteroids ++")
    debug.textLine("")
    debug.textLine("")
    val playButton = debug.textLine("[ENTER]")

    screenView.addChild(title)
    screenView.addChild(playButton)

    screenView.position((stage.unscaledWidth / 2.0) - screenView.width, (stage.unscaledHeight / 2.0) - screenView.height).addTo(stage.containerRoot)

    playButton.keys.down {
        if (it.key == Key.ENTER) {
            stage.removeAllComponents()
            stage.removeChildren()
            startNewGame(stage)
        }
    }
    playButton.onClick {
        stage.removeAllComponents()
        stage.removeChildren()
        startNewGame(stage)
    }
}

private suspend fun startNewGame(stage: Stage) {
    stage.setupStarField(SolidRect(width = 1.0, height = 1.0, color = Colors.WHITE), 0.01)
    val game = Game(stage, stage) { game ->

        showEndgameScreen(stage, game)
    }
    game.start()
}

/**
 * [density] value of 1.0 means a star per pixel randomly placed in the container.
 */
private fun Container.setupStarField(star: View, density: Double) {
    val numberOfStars = (this.unscaledWidth * this.unscaledHeight * density).toInt()
    repeat(numberOfStars) {
        this.addChild(
                star.clone().position(
                        Random.nextInt((unscaledWidth + 1).toInt()),
                        Random.nextInt((unscaledHeight + 1).toInt())
                ).alpha(Random.nextDouble())
        )
    }
}

var bestScore = 0

private fun showEndgameScreen(stage: Stage, game: Game) {
    println("Game Over")
    val score = (game.wave * 100) + (game.asteroidsKilled * 50)
    if (score > bestScore) bestScore = score

    val screenView = Container()
    val debug = Debug(screenView)
    val title = debug.textLine("[Play Again]")
    debug.textLine("")
    debug.textLine(("Score: ${game.score}"))
    debug.textLine(("Wave: ${game.wave}"))
    debug.textLine(("Asteroids destroyed: ${game.asteroidsKilled}"))
    debug.textLine(("Tanks collected: ${game.tanksCollected.size}"))
    debug.textLine("")
    debug.textLine("Best Score: $bestScore")

    screenView.position((stage.unscaledWidth / 2.0) - screenView.width, (stage.unscaledHeight / 2.0) - screenView.height).addTo(stage.containerRoot)
    title.keys.down {
        if (it.key == Key.ENTER) {
            stage.removeAllComponents()
            stage.forEachChild { child -> child.removeAllComponents() }
            stage.removeChildren()
            startNewGame(stage)
        }
    }
    title.onClick {
        stage.removeAllComponents()
        stage.forEachChildren { it.removeAllComponents() }
        stage.removeChildren()
        startNewGame(stage)
    }
}