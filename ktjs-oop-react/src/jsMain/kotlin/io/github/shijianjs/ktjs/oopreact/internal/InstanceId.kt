package io.github.shijianjs.ktjs.oopreact.internal

import kotlin.js.JsAny

/**
 * A stable id per object, used in the default React key.
 *
 * Every island element has the same component type, so without a key React reuses the fiber when a
 * parent swaps one island for another — for instance when a route renders a different page. The same
 * fiber would then run a different render method, and a different hook count mid-fiber is something
 * React rejects. The id is what makes "this island" and "that island" distinguishable.
 */
private external interface WeakMapLike {
    fun get(key: JsAny): Int?

    fun set(key: JsAny, value: Int)
}

private val instanceIds: WeakMapLike = js("new WeakMap()")

private var nextInstanceId: Int = 0

internal fun instanceId(instance: JsAny): Int {
    val existing = instanceIds.get(instance)
    if (existing != null) return existing

    nextInstanceId += 1
    instanceIds.set(instance, nextInstanceId)
    return nextInstanceId
}
