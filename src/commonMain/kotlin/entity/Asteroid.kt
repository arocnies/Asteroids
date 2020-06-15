package entity

import com.soywiz.klock.TimeSpan
import com.soywiz.korge.view.Sprite
import com.soywiz.korge.view.addFixedUpdater
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.launchImmediately

class Asteroid(mass: Double = 2000.0, sprite: Sprite) : MassObject(mass, sprite) {
    fun hit() {
        sprite.addFixedUpdater(TimeSpan(100.0)) {
            sprite.alpha = alpha - 0.2
            if (alpha < 0.0) {
                // Destroy
                sprite.removeFromParent()
                sprite.removeAllComponents()
            }
        }
    }
}