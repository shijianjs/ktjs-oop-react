package io.github.shijianjs.ktjs.oopreact.render

/**
 * Turns a method that returns JSX into a self-contained, subscribed React subtree.
 *
 * The decorated method keeps its signature, so a caller still writes `this.view()`; what changes is
 * that the returned element re-renders on its own whenever the state *that method read* changes.
 * Everything outside an annotated method — the plain fields, the methods that mutate them, the
 * callbacks they hand to JSX — is ordinary object code and never re-runs on a render.
 *
 * ```kotlin
 * @OopReactState
 * class Cart {
 *     val items = OopReactList<Item>()
 *
 *     @OopReactRender
 *     @JsName("view")
 *     fun view(): ReactNode = …
 * }
 * ```
 *
 * **`@JsName` is required**, and it has to match the Kotlin name. Kotlin/JS compiles a member to
 * `view_<hash>_k$`, and the hash is not predictable from source — so the runtime cannot find the
 * method by the name the caller writes. `@JsName("view")` pins the JavaScript name; the processor
 * rejects a `@OopReactRender` method without one, because the annotation would otherwise silently do
 * nothing.
 *
 * A render method must not write state: it runs while React renders, and a write would invalidate the
 * subscription it is running inside. Counting renders is a side effect too — keep it in a plain
 * object outside the instance.
 *
 * @param name the owner name shown by the fallback and reported by `onError`; defaults to the class
 *   name, which a production build minifies into something unrecognisable. Pass the name a reader
 *   would recognise when the island is one a human will see fail.
 * @param key the React key of one list entry, read as a property. It is looked up on the first
 *   argument when the method takes one (a parent rendering rows for its children) and on the
 *   instance otherwise; a method found there is called with the instance and the arguments:
 *
 *   ```kotlin
 *   @OopReactRender(key = "id")            // item.id, or this.id
 *   @OopReactRender(key = "rowKey")        // fun rowKey(item: Row): String
 *   ```
 *
 *   The key has to be produced by the island, not returned from the render function: the element is
 *   created before the method runs, and React reads the key from that element when the *parent*
 *   reconciles its children.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class OopReactRender(
    val name: String = "",
    val key: String = "",
)
