package io.github.shijianjs.ktjs.oopreact.render

import io.github.shijianjs.ktjs.oopreact.internal.observer
import react.ChildrenBuilder
import react.FC
import react.Props
import react.ReactNode

/** What [TrackedRenderElement] renders: called while React renders, inside a MobX derivation. */
external interface OopReactTrackedRenderProps : Props {
    /**
     * The function whose observable reads decide when this component re-renders.
     *
     * It must not write state: it is called while React renders, and a write would invalidate the
     * subscription it is currently running inside.
     */
    var render: () -> ReactNode?
}

/**
 * The whole reactivity contract of the library, in one component.
 *
 * Tracking is `observer` from `mobx-react-lite` — the official React binding — rather than a
 * hand-rolled `Reaction`: it subscribes the render to exactly the fields it reads, batches updates
 * through React's own machinery, and re-throws what the render throws so an error boundary above it
 * can catch the failure.
 *
 * It is public because it is the primitive everything else is built on: an island is this component
 * plus an error boundary plus a memo.
 */
val OopReactTrackedRender: FC<OopReactTrackedRenderProps> =
    observer(
        FC<OopReactTrackedRenderProps> { props ->
            +props.render()
        },
    )

/** Adds a tracked render to a children block. */
internal fun ChildrenBuilder.TrackedRenderElement(render: () -> ReactNode?) {
    OopReactTrackedRender {
        this.render = render
    }
}
