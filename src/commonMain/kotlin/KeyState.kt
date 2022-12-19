import com.soywiz.korev.EventDispatcher
import com.soywiz.korev.EventListener
import com.soywiz.korev.Key
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.views
import com.soywiz.korio.async.runBlockingNoSuspensions
import com.soywiz.korio.dynamic.dyn

/**
 * Maintains the state of all down keys for easy lookup using [get].
 */
class KeyState {
    operator fun get(key: Key): Boolean = runBlockingNoSuspensions {
        views().input.keys.pressing(key)
    }
}