@file:JsModule("mobx")

package io.github.shijianjs.ktjs.oopreact.reactivity.mobx

import js.array.ReadonlyArray
import kotlin.js.JsAny
import kotlin.js.JsName

/**
 * The slice of MobX this library actually uses.
 *
 * Reactivity is MobX itself, not an abstraction over it: an instance's fields are made observable
 * with MobX's own `makeObservable` and the same auto annotations `makeAutoObservable` would apply.
 * Everything else in the MobX API is left to the application — it depends on `mobx` directly, which
 * is why the library declares it as a normal dependency instead of re-exporting it.
 *
 * The declarations are `internal`: `mobx` is an implementation detail of this module, and a public
 * re-export would freeze a second Kotlin-level API onto it.
 */

internal external fun makeObservable(target: JsAny, annotations: JsAny, options: JsAny)

@JsName("makeObservable")
internal external fun makeObservableWithoutOptions(target: JsAny, annotations: JsAny)

internal external fun isObservable(value: JsAny?): Boolean

internal external fun configure(options: JsAny)

internal external fun autorun(effect: () -> Unit): () -> Unit

internal external fun runInAction(block: () -> Unit): JsAny?

/**
 * A MobX reaction. The tracked function is supplied by [track], which is what makes a render method
 * subscribe to exactly the fields it reads.
 */
internal external class Reaction(
    name: String?,
    onInvalidate: () -> Unit,
) {
    fun track(fn: () -> Unit)

    fun dispose()

    /** Re-runs the reaction after an invalidation, skipping MobX's own batching queue. */
    fun runReaction_()
}

/** The observable array `observable.array` builds: a real JS array, instrumented by MobX. */
internal external interface ObservableArray {
    var length: Int

    fun push(vararg items: JsAny?): Int

    /** The only way to remove an element: `delete items[i]` leaves a hole MobX cannot track. */
    fun splice(start: Int, deleteCount: Int, vararg items: JsAny?): Array<JsAny?>
}

internal external interface ObservableFactory {
    fun array(initialValues: ReadonlyArray<JsAny?>?): ObservableArray
}

internal external val observable: ObservableFactory
