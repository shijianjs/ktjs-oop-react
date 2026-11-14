package io.github.shijianjs.ktjs.oopreact.render

import io.github.shijianjs.ktjs.oopreact.internal.castFrom
import io.github.shijianjs.ktjs.oopreact.internal.instanceId
import io.github.shijianjs.ktjs.oopreact.internal.observer
import js.array.ReadonlyArray
import io.github.shijianjs.ktjs.oopreact.error.OopReactErrorBoundary
import io.github.shijianjs.ktjs.oopreact.internal.applyFunction
import io.github.shijianjs.ktjs.oopreact.internal.isFunction
import io.github.shijianjs.ktjs.oopreact.internal.readProperty
import io.github.shijianjs.ktjs.oopreact.internal.wrapMethod
import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactRenderMeta
import io.github.shijianjs.ktjs.oopreact.reactivity.ensureObservable
import io.github.shijianjs.ktjs.oopreact.reactivity.islandName
import kotlin.js.JsAny
import react.FC
import react.Key
import react.Props
import react.ReactNode
import react.create
import react.memo

/**
 * The React element a render method hands back to its caller.
 *
 * Two things live here, and both have to wrap **each** island rather than a component as a whole:
 * the subscription (so only the fields this method read trigger a re-render) and the error boundary
 * (so one broken method does not take the page down).
 *
 * The element is created by the patched method, before the method body runs, so rendering it is
 * side-effect free and it can be produced from any render: a plain React component, another island,
 * a parent object's method.
 */
internal external interface OopReactIslandProps : Props {
    /** The object the render method belongs to; part of the island's identity. */
    var owner: JsAny

    /** The original (unpatched) render method; part of the island's identity. */
    var method: JsAny

    /** The call arguments; compared element-wise, so an unchanged call does not re-render. */
    var args: ReadonlyArray<JsAny?>

    /** `<Owner>.<method>`, shown by the fallback and reported by `onError`. */
    var islandName: String
}

/**
 * Islands are memoized on what actually identifies them: the instance, the method, the name and the
 * call arguments. A parent island re-rendering therefore does **not** re-run a child island whose
 * arguments did not change — the child still re-renders on its own when a field *it* read changes.
 *
 * The arguments are compared, not the objects inside them: mutating a field of an argument object is
 * caught by the child's own subscription, which is exactly the case this must not skip.
 *
 * The comparison is ours rather than `observer`'s: `observer` memoizes on props with a shallow
 * comparison, and `args` is a fresh array on every call, so it would never skip.
 */
private fun sameIsland(previous: OopReactIslandProps, next: OopReactIslandProps): Boolean =
    previous.owner === next.owner &&
        previous.method === next.method &&
        previous.islandName == next.islandName &&
        sameArgs(previous.args, next.args)

private fun sameArgs(previous: ReadonlyArray<JsAny?>, next: ReadonlyArray<JsAny?>): Boolean {
    if (previous === next) return true
    if (previous.size != next.size) return false
    for (index in 0 until previous.size) {
        if (previous[index] !== next[index]) return false
    }
    return true
}

/**
 * The bound render method: one per class member, created once when the prototype is patched.
 *
 * `islandName` and the key are resolved per call, not per class: the owner name may come from the
 * component that mounted the object, and a key belongs to one list entry.
 */
internal val OopReactIsland: FC<OopReactIslandProps> =
    memo(
        observer(
            FC<OopReactIslandProps> { props ->
                OopReactErrorBoundary(props.islandName) {
                    TrackedRenderElement {
                        castFrom<ReactNode?>(applyFunction(props.method, props.owner, props.args))
                    }
                }
            },
        ),
        ::sameIsland,
    )

/**
 * Builds the function that replaces `meta.jsName` on the prototype.
 *
 * Everything the wrapper does happens on call, because the receiver is only known then: make the
 * instance observable (once), resolve the island's name and key, and return the element.
 */
internal fun oopReactIslandMethod(original: JsAny, meta: OopReactRenderMeta): JsAny =
    wrapMethod(original) { receiver, method, args ->
        ensureObservable(receiver)
        createIsland(receiver, method, args, meta)
    }

private fun createIsland(
    receiver: JsAny,
    method: JsAny,
    args: ReadonlyArray<JsAny?>,
    meta: OopReactRenderMeta,
): ReactNode {
    val name = islandName(receiver, meta)
    val key = resolveKey(meta, receiver, args)

    return OopReactIsland.create {
        owner = receiver
        this.method = method
        this.args = args
        islandName = name
        this.key = key
    }
}

/**
 * `key = "id"` reads that property — from the first argument when the method takes one (a parent
 * rendering rows for its children), otherwise from the instance. A method found there is called with
 * the instance and the arguments, which is how a derived key is spelled:
 *
 * ```kotlin
 * @JsName("rowKey")
 * fun rowKey(item: Row): String = "${item.category}/${item.id}"
 *
 * @OopReactRender(key = "rowKey")
 * @JsName("row")
 * fun row(item: Row): ReactNode
 * ```
 *
 * A missing key falls back to `<instanceId>.<method>`, which keeps React from reusing one island's
 * fiber for a *different* island in the same position — that would change the hook count mid-fiber.
 */
private fun resolveKey(
    meta: OopReactRenderMeta,
    receiver: JsAny,
    args: ReadonlyArray<JsAny?>,
): Key? {
    val fallback = Key("${instanceId(receiver)}.${meta.jsName}")
    val name = meta.key ?: return fallback

    val fromArgument = if (args.size > 0) readProperty(args[0], name) else null
    val value = fromArgument ?: readProperty(receiver, name) ?: return fallback

    val resolved = if (isFunction(value)) applyFunction(value, receiver, args) else value
    return when (resolved) {
        null -> fallback
        is String -> Key(resolved)
        is Int -> Key(resolved)
        else -> castFrom(resolved)
    }
}
