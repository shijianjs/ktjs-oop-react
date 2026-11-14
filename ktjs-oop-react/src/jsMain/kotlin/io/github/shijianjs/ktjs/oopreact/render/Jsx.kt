package io.github.shijianjs.ktjs.oopreact.render

import react.ChildrenBuilder
import react.Fragment
import react.ReactNode
import react.create

/**
 * The render method's body — the Kotlin equivalent of a JSX fragment.
 *
 * A render method returns a `ReactNode`, but the element DSL (`div { … }`, `Button { … }`) only
 * exists inside a `ChildrenBuilder`, which is the block a *component* receives. This function is
 * that block, wrapped into a single node:
 *
 * ```kotlin
 * @OopReactRender
 * @JsName("view")
 * fun view(): ReactNode = jsx {
 *     Typography { +"count=$count" }
 *     Button { onClick = { count += 1 }; +"+" }
 * }
 * ```
 *
 * It is a fragment, not a wrapper element: nothing is added to the DOM, and the children are exactly
 * the ones the island renders.
 */
fun jsx(block: ChildrenBuilder.() -> Unit): ReactNode =
    Fragment.create(block)
