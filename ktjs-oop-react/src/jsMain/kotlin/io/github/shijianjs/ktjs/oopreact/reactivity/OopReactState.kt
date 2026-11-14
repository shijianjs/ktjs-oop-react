package io.github.shijianjs.ktjs.oopreact.reactivity

/**
 * Declares a class as state, so its instances are made observable before they are first rendered.
 *
 * ```kotlin
 * @OopReactState
 * class Cart {
 *     val items = OopReactList<Item>()
 *     var open = false
 *
 *     @OopReactRender
 *     @JsName("view")
 *     fun view(): ReactNode = …
 * }
 * ```
 *
 * Every field becomes observable, every method an action bound to the instance, and the whole
 * prototype chain is covered — so a subclass inherits the annotation and contributes its own fields
 * without a second call.
 *
 * The annotation itself does nothing at runtime: Kotlin/JS has no runtime annotation reflection, so
 * `ktjs-oop-react-ksp` collects it at build time and the generated `initializeOopReact()` registers
 * the class. Apply that function once at application start, or an annotated class behaves like a
 * plain one.
 *
 * Observability is established **lazily**, on the first call of a render method (or the first
 * `oopReactObservable(instance)`), not in the constructor. That is what makes inheritance work
 * without the decorator trick the TypeScript original needs: by then every field of every class in
 * the chain exists. Nothing observes the object before that point, so no update is missed.
 *
 * Eager establishment is available by calling [oopReactObservable] as the last statement of the most
 * derived class's `init` block — necessary only when construction itself has to react to a field, for
 * instance a `reaction` started there.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class OopReactState
