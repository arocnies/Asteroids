package entity

import com.soywiz.korge.animate.animate
import com.soywiz.korge.view.Sprite
import com.soywiz.korge.view.addUpdater

class Asteroid(mass: Double = 2000.0, sprite: Sprite) : MassObject(mass, sprite) {
    fun hit() {
        // Destroy
        sprite.removeFromParent()
        sprite.removeAllComponents()
    }
}