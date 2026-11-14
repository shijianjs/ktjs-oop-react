package io.github.shijianjs.ktjs.oopreact.internal

import io.github.shijianjs.ktjs.oopreact.config.OopReactEnforceActions
import io.github.shijianjs.ktjs.oopreact.reactivity.mobx.configure
import kotlin.js.JsAny

/** `true` is MobX's "pick the right annotation yourself" marker, used as the value of a key map. */
internal val AUTO_ANNOTATION: JsAny = js("true")

/** Sets MobX's `enforceActions` without exposing the MobX options type. */
internal fun configureEnforceActions(value: OopReactEnforceActions) {
    val mode = value.mobxValue
    configure(js("({enforceActions: mode})"))
}

private var duplicateReported: Boolean = false

/**
 * Warns when the page holds more than one copy of MobX.
 *
 * Two copies cannot see each other: MobX keys its bookkeeping on a symbol made per module instance,
 * so a reaction from one copy never subscribes to a value created by the other. The failure mode is
 * the bad one — no error, no warning from MobX itself (it only complains about *incompatible*
 * versions), just "the state changed and the UI did not".
 *
 * `__mobxInstanceCount` is MobX's own counter of loaded copies and has existed since MobX 4, so it is
 * stable across the versions this library supports.
 */
internal fun reportDuplicateMobx() {
    if (duplicateReported) return

    val count: Int = js(
        "typeof globalThis.__mobxInstanceCount === 'number' ? globalThis.__mobxInstanceCount : 0",
    )
    if (count < 2) return

    duplicateReported = true
    console.warn(
        "[oop-react] $count copies of mobx are loaded in this page. Each copy only tracks the observables it " +
            "created, so an island rendered under one copy never re-renders when another copy's state changes — " +
            "the classic \"state changed, UI did not\" bug. Keep exactly one copy: run `npm ls mobx`, then dedupe " +
            "or pin a single version through overrides/resolutions.",
    )
}
