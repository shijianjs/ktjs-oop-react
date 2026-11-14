package io.github.shijianjs.ktjs.oopreact.error

import io.github.shijianjs.ktjs.oopreact.config.oopReactConfig
import io.github.shijianjs.ktjs.oopreact.internal.castFrom
import kotlin.js.JsAny
import react.ChildrenBuilder
import react.Component
import react.ComponentType
import react.ErrorInfo
import react.FC
import react.PropsWithChildren
import react.ReactNode
import react.State
import react.create

/** Props of the boundary itself: the island name it reports, plus whatever it wraps. */
external interface OopReactErrorBoundaryProps : PropsWithChildren {
    /** `<Owner>.<method>`; reported to `onError` and passed to the fallback. */
    var name: String?
}

/** What the boundary holds between the throw and the render of the fallback. */
private external interface OopReactErrorBoundaryState : State {
    var failed: Boolean
    var error: JsAny?
}

/**
 * One boundary per render method, so a broken island renders a fallback in its own place and leaves
 * every sibling — including the rest of the same component — intact.
 *
 * The boundary is a class component because React has no hook equivalent; it holds no application
 * state, only "did this subtree throw".
 *
 * It reacts in `componentDidCatch` rather than in `getDerivedStateFromError`: the latter is a
 * *static* member, and Kotlin cannot declare a static on a subclass of an external class. The
 * difference is one extra render pass on failure, which is invisible next to the failure itself.
 */
private class OopReactErrorBoundaryImpl(
    props: OopReactErrorBoundaryProps,
) : Component<OopReactErrorBoundaryProps, OopReactErrorBoundaryState>(props) {

    init {
        state = emptyBoundaryState()
    }

    override fun componentDidCatch(error: Throwable, info: ErrorInfo) {
        val thrown: JsAny? = castFrom(error)
        oopReactConfig().onError(thrown, props.name)
        setState(failedBoundaryState(thrown))
    }

    override fun render(): ReactNode? {
        val current = state
        if (!current.failed) return props.children

        val config = oopReactConfig()
        val reset = { setState(emptyBoundaryState()) }

        return config.fallback.create {
            error = current.error
            name = props.name
            this.reset = reset
        }
    }
}

private val OopReactErrorBoundaryType: ComponentType<OopReactErrorBoundaryProps> =
    castFrom(OopReactErrorBoundaryImpl::class.js)

/**
 * Wraps a subtree in an error boundary — the same boundary an island gets, for hand-written JSX.
 *
 * ```kotlin
 * OopReactErrorBoundary(name = "Chart") { chart.view() }
 * ```
 */
fun ChildrenBuilder.OopReactErrorBoundary(
    name: String?,
    content: ChildrenBuilder.() -> Unit,
) {
    OopReactErrorBoundaryType {
        this.name = name
        content()
    }
}

/** The ordinary `PropsWithChildren` shape, for callers that build the boundary with the DSL. */
val OopReactErrorBoundaryComponent: FC<OopReactErrorBoundaryProps> =
    FC { props ->
        OopReactErrorBoundaryType {
            name = props.name
            children = props.children
        }
    }

private fun emptyBoundaryState(): OopReactErrorBoundaryState =
    castFrom(js("({failed: false, error: null})"))

private fun failedBoundaryState(error: JsAny?): OopReactErrorBoundaryState {
    val thrown = error
    return castFrom(js("({failed: true, error: thrown})"))
}
