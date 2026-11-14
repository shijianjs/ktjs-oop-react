package io.github.shijianjs.ktjs.oopreact.reactivity

/**
 * Keeps a field as a plain field: not observable, not tracked.
 *
 * ```kotlin
 * @OopReactState
 * class Cart {
 *     @OopReactIgnore
 *     @JsName("api")
 *     val api = ApiClient()
 * }
 * ```
 *
 * This exists because "everything except these two" is the common shape, and MobX's own annotations
 * are the opposite — you whitelist every observable field instead. The mark is inherited: a subclass
 * is covered by the same mark, and an entry in `oopReactObservable`'s `overrides` still wins for a
 * one-off case.
 *
 * **`@JsName` is required**, and the processor rejects the field without it. Kotlin/JS renames a
 * private field to `api_1` at compile time; the runtime finds members by their JavaScript name, and
 * `@JsName` is the only way to pin it.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
annotation class OopReactIgnore
