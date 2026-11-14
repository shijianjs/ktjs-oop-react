package io.github.shijianjs.ktjs.oopreact.reactivity

/**
 * Makes a method one transaction: every field it writes while running is applied as a single change,
 * so readers see the before or the after — never the middle.
 *
 * ```kotlin
 * @OopReactAction
 * @JsName("reset")
 * fun reset() {
 *     customer = ""
 *     amount = 0
 *     saving = false    // one re-render, not three
 * }
 * ```
 *
 * In most classes this is redundant: `@OopReactState` already treats every method as a bound action.
 * It earns its keep when the class was made observable with `autoBind = false`, when an entry in
 * `oopReactObservable`'s `overrides` opts the method out, or when the intent is worth spelling out at
 * the method.
 *
 * `@JsName` is required for the same reason as on `@OopReactRender`: the runtime has to find the
 * method by its JavaScript name.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class OopReactAction
