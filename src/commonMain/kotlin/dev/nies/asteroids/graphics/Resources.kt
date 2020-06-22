package dev.nies.asteroids.graphics

import com.soywiz.korge.resources.ResourcesRoot

class ResourceCache {
    fun <T> include(resourceItem: ResourceItem<T>) {
    }

    suspend fun loadResources(progress: (Double) -> Unit) {

    }
}

class ResourceItem<T>(val name: String, val load: suspend () -> Unit) {

}
