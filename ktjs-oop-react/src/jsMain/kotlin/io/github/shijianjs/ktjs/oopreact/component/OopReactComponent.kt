package io.github.shijianjs.ktjs.oopreact.component

import io.github.shijianjs.ktjs.oopreact.internal.asAny
import io.github.shijianjs.ktjs.oopreact.internal.asJsAny
import io.github.shijianjs.ktjs.oopreact.internal.setComponentName
import io.github.shijianjs.ktjs.oopreact.reactivity.oopReactObservable
import kotlin.js.JsAny
import react.FC
import react.Props
import react.ReactNode
import react.useEffect
import react.useRef

/**
 * Mounts one render method of a class as a React component — the bridge between an object graph and
 * the React tree.
 *
 * ```kotlin
 * // the only React-aware line in an otherwise plain-object application
 * val WorkshopView = oopReactComponent<WorkshopProps, Workshop>(
 *     name = "Workshop",
 *     instance = { props -> Workshop(props.title) },
 * ) {
 *     view()
 * }
 * ```
 *
 * This is not the main way to use the library. The main way is to `new` state objects wherever they
 * belong, let them hold each other, and call their `@OopReactRender` methods from a component that
 * already exists. Reach for this when a class has to *be* a component: adopting the library one
 * screen at a time in a React app, or exposing a piece of the graph to a page that only speaks props.
 *
 * The instance lives as long as the component is mounted and is never rebuilt; later props arrive
 * through [onPropsChange] rather than through a new instance.
 *
 * @param name the name this component and its islands report: DevTools shows `<name>.<method>`, and a
 *   fallback shows the island it came from. Defaults to the class name, which a production build
 *   minifies — pass the name a reader would recognise.
 * @param autoObservable whether to declare the instance's state here. `true` (the default) is what
 *   makes an *existing* plain class usable as-is; a class that already calls `oopReactObservable` in
 *   its `init` needs nothing, the second call is a no-op.
 * @param onPropsChange called after mount and whenever `props` change, for a subclass that has to
 *   react to a new input rather than be rebuilt.
 */
fun <P : Props, I : Any> oopReactComponent(
    name: String,
    autoObservable: Boolean = true,
    instance: (P) -> I,
    onPropsChange: ((instance: I, props: P, previousProps: P) -> Unit)? = null,
    view: I.() -> ReactNode?,
): FC<P> {
    val component = FC<P> { props ->
        val instanceRef = useRef<I>()
        if (instanceRef.current == null) {
            val created = instance(props)
            instanceRef.current = if (autoObservable) oopReactObservable(created) else created
            setComponentName(created.asJsAny(), name)
        }
        val current = instanceRef.current!!

        val previousProps = useRef<JsAny>()
        useEffect(props.asAny()) {
            val previous: JsAny? = previousProps.current
            previousProps.current = props.asJsAny()
            if (previous != null) onPropsChange?.invoke(current, props, previous.asAny())
        }

        +current.view()
    }

    component.displayName = name
    return component
}
