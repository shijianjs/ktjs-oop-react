package demo.app

import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState

/**
 * What the page tells the user when something happened: one line, shown in a snackbar.
 *
 * A tiny object rather than a pile of callbacks: any object in the graph can hold a reference to it
 * and call [show], and the snackbar island re-renders — nothing else does.
 */
@OopReactState
class Feedback {
    var message: String? = null

    var open: Boolean = false

    fun show(text: String) {
        message = text
        open = true
    }

    fun dismiss() {
        open = false
    }
}
