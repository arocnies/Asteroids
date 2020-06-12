import com.soywiz.korev.EventDispatcher
import com.soywiz.korev.Key
import com.soywiz.korev.keys

/**
 * Maintains the state of all down keys for easy lookup using [get].
 */
class KeyState(eventDispatcher: EventDispatcher) {
    private val downKeys = mutableSetOf<Key>()

    init {
        eventDispatcher.keys {
            down {
                downKeys += key
            }
            up {
                downKeys -= key
            }
        }
    }

    operator fun get(key: Key): Boolean = downKeys.contains(key)
}