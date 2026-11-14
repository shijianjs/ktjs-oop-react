package io.github.shijianjs.ktjs.oopreact.render

import react.Key
import react.ReactNode

/**
 * Wraps a function that returns JSX into a callback that renders an island when it is called.
 *
 * It exists for the shape a component-in-a-prop needs:
 *
 * ```kotlin
 * val renderRow = OopReactCallback<Row>({ row -> li { +row.title } }, key = { it.id })
 * ul { rows.forEach { +renderRow(it) } }
 * ```
 *
 * The key belongs to the callback, not to the element inside it: the island is what sits in the
 * parent's list, so a `key` on the `<li>` would not help. The returned function renders a component,
 * so it must be called during a render — as a render prop is — not from an event handler.
 */
fun <A> OopReactCallback(
    key: ((A) -> Key?)? = null,
    render: (A) -> ReactNode?,
): (A) -> ReactNode? =
    { argument -> OopReactObserver(key?.invoke(argument)) { render(argument) } }
