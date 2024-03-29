package debug

import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.filter
import kotlin.reflect.KProperty

/**
 * Updates text in the provided [container] on each frame.
 */
class Debug(val container: Container) {
    var line = 0
    fun textLine(text: String) = container.text(text, textSize = 28.0).position(14, 14 + (line++ * 42)).apply { filter = null }

    /**
     * Requires reflection in order to display property value unlike the other [track] functions
     */
    fun track(prop: KProperty<*>) {
        val propText = textLine(prop.name)
        container.addUpdater {
            propText.text = prop.toString()
        }
    }
    fun track(getValue: () -> Any?) {
        val debugLine = textLine("")
        container.addUpdater {
            debugLine.text = getValue().toString()
        }
    }
    fun track(prop: KProperty<*>, getValue: () -> Any?) {
        val debugLine = textLine(prop.name)
        container.addUpdater {
            debugLine.text = "${prop.name}: ${getValue().toString()}"
        }
    }
}
