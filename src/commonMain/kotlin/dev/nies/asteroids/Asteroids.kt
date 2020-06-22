package dev.nies.asteroids

import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Module
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korma.geom.SizeInt

suspend fun main() = Korge(Korge.Config(module = Asteroids))

object Asteroids : Module() {
    override val mainScene = StartMenu::class
    override val size = SizeInt(1600, 900)

    override suspend fun AsyncInjector.configure() {
        mapPrototype { StartMenu() }
        mapPrototype { PlayAsteroids() }
    }
}