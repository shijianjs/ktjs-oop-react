package io.github.shijianjs.ktjs.oopreact.render

import react.Key
import react.ReactNode
import react.create

/**
 * Subscribes any function that returns JSX to the observable state it reads.
 *
 * This is the primitive `@OopReactRender` is built on. It is public for the few places where a class
 * method is not the natural unit — a render prop, an inline block inside an existing component — and
 * it must be called while a component renders, because it renders a component itself.
 *
 * ```kotlin
 * fun Total() = FC {
 *     +OopReactObserver { strong { +"${cart.items.size}" } }
 * }
 * ```
 *
 * @param key the identity of this block among its siblings — the same idea as `@OopReactRender(key)`.
 *   It has to be given here, not returned from the render function: React compares keys among the
 *   parent's children, and the content is only known after the render function has run.
 */
fun OopReactObserver(
    key: Key? = null,
    render: () -> ReactNode?,
): ReactNode =
    OopReactTrackedRender.create {
        this.key = key
        this.render = render
    }
