package io.github.shijianjs.ktjs.oopreact.render

import io.github.shijianjs.ktjs.oopreact.internal.asAny
import kotlinx.coroutines.awaitCancellation
import react.useEffect

/**
 * Hands a React-context-bound value to an object.
 *
 * It exists because a component library's most useful instances only exist *inside* a component: a form
 * API, a dialog controller, a theme. An object cannot call a hook, but it can **hold** the result — and
 * then any method can drive the form or open the dialog. This function is the only place where the
 * hand-over is allowed to happen: after commit, in an effect, never during a render.
 *
 * ```kotlin
 * @OopReactRender
 * @JsName("host")
 * fun host(): ReactNode = jsx {
 *     // inside a component that owns the hook
 *     OopReactBridge(form, assign = { editor.bindForm(it) }, release = { editor.unbindForm() })
 * }
 * ```
 *
 * Call it from a render method (it registers an effect on the island it belongs to, not a component of
 * its own). `assign` runs on mount and whenever `value` changes; `release` runs before a new value is
 * assigned and on unmount.
 */
fun <V : Any> OopReactBridge(
    value: V,
    assign: (V) -> Unit,
    release: () -> Unit = {},
) {
    useEffect(value.asAny()) {
        assign(value)
        try {
            // The effect body is cancelled on unmount, which is where `release` becomes due.
            awaitCancellation()
        } finally {
            release()
        }
    }
}
