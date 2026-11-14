package io.github.shijianjs.ktjs.oopreact.config

import kotlin.js.JsAny
import react.Props

/** What a fallback component is handed. */
external interface OopReactFallbackProps : Props {
    /** Whatever the render method threw. */
    var error: JsAny?

    /** `<ClassName>.<methodName>`, or the name the island was given. */
    var name: String?

    /** Clears the error and renders the method again. */
    var reset: () -> Unit
}
