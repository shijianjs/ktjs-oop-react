package io.github.shijianjs.ktjs.oopreact.config

import io.github.shijianjs.ktjs.oopreact.error.DefaultErrorFallback
import io.github.shijianjs.ktjs.oopreact.internal.configureEnforceActions
import kotlin.js.JsAny
import react.ComponentType

/** The value MobX uses for `enforceActions`, spelled the way MobX accepts it. */
enum class OopReactEnforceActions(
    internal val mobxValue: String,
) {
    /**
     * Any field may be written anywhere (the default).
     *
     * Methods are still actions, so writing three fields in one method is still one re-render —
     * what is switched off is MobX's per-write warning, not the batching.
     */
    Never("never"),

    /** MobX's default: writing an *observed* field outside an action warns. */
    Observed("observed"),

    /** Every write outside an action is an error. */
    Always("always"),
}

/** What an application may replace: how a failed island looks, where it is reported, and how strict MobX is. */
class OopReactConfig internal constructor(
    /** Rendered in place of a render method that threw. */
    val fallback: ComponentType<OopReactFallbackProps>,
    /** Called once per caught render error. */
    val onError: (error: JsAny?, name: String?) -> Unit,
    /** MobX's `enforceActions`; see [OopReactEnforceActions]. */
    val enforceActions: OopReactEnforceActions,
)

/** The replaceable parts of [OopReactConfig]; `null` keeps what is configured now. */
class OopReactConfigPatch(
    val fallback: ComponentType<OopReactFallbackProps>? = null,
    val onError: ((error: JsAny?, name: String?) -> Unit)? = null,
    val enforceActions: OopReactEnforceActions? = null,
)

private var current: OopReactConfig? = null

private fun defaults(): OopReactConfig =
    OopReactConfig(
        fallback = DefaultErrorFallback,
        onError = { error, name ->
            console.error("[oop-react] ${name ?: "a render method"} threw", error)
        },
        enforceActions = OopReactEnforceActions.Never,
    )

/**
 * The active configuration, established on first read.
 *
 * The default `enforceActions` is applied here rather than at class-load time because Kotlin/JS runs
 * a top-level initialiser when the module is first touched, and "first touched" may well be a state
 * write. That is harmless: MobX only polices writes to fields something observes, and nothing is
 * observed before the first render — which is exactly when this runs.
 */
internal fun oopReactConfig(): OopReactConfig {
    val existing = current
    if (existing != null) return existing

    val initial = defaults()
    current = initial
    configureEnforceActions(initial.enforceActions)
    return initial
}

/**
 * Replaces part of the default implementation and returns a function that restores what was
 * configured before — which is what a test uses to undo its own override.
 *
 * ```kotlin
 * val restore = oopReactConfigure(OopReactConfigPatch(fallback = MyFallback, enforceActions = OopReactEnforceActions.Always))
 * ```
 */
fun oopReactConfigure(patch: OopReactConfigPatch = OopReactConfigPatch()): () -> Unit {
    val previous = oopReactConfig()
    current =
        OopReactConfig(
            fallback = patch.fallback ?: previous.fallback,
            onError = patch.onError ?: previous.onError,
            enforceActions = patch.enforceActions ?: previous.enforceActions,
        )

    if (patch.enforceActions != null) {
        configureEnforceActions(patch.enforceActions)
    }

    return {
        current = previous
        configureEnforceActions(previous.enforceActions)
    }
}
