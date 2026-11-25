package io.github.shijianjs.ktjs.oopreact.internal

import js.array.ReadonlyArray
import js.objects.Object
import js.objects.PropertyKey
import js.reflect.Reflect
import kotlin.js.JsAny

/**
 * The JavaScript operations this library needs on values it only knows as `JsAny`.
 *
 * The operations the official Kotlin/JS typing already covers go through it — `js.reflect.Reflect`,
 * `js.objects.Object`, `jsTypeOf` — rather than being spelled out as JS snippets. What is still a
 * `js(...)` snippet is only what has no typed counterpart at all: the enclosing function's `this`,
 * a call with an explicit receiver, and building a real (non-lambda) wrapper function.
 */

/** The JS `this` of the enclosing function. Only valid inside a real function, never a lambda. */
internal fun jsThis(): JsAny =
    js("this")

/**
 * Crosses the `Any` / `JsAny` boundary.
 *
 * Kotlin/JS keeps the two type systems apart on purpose: a Kotlin class is an `Any`, a JS value is a
 * `JsAny`, and neither is declared as a subtype of the other. Every crossing in this library goes
 * through one of these two functions, so the unsafe step is visible at the few places it happens.
 */
@Suppress("UNCHECKED_CAST")
internal fun <T> castFrom(value: Any?): T =
    value as T

/**
 * Turns any value into a `JsAny` without a runtime check.
 *
 * A plain `as JsAny` would emit `!= null`-style validation on some values; this must not, because
 * the value may be a JS primitive (a string, a number) that is perfectly valid on the JS side.
 */
internal fun Any?.asJsAny(): JsAny =
    unsafeCast<JsAny>()

/** The other direction: a JS value used where Kotlin expects `Any`. */
internal fun <T> JsAny?.asAny(): T =
    castFrom(this)

internal fun prototypeOf(constructor: JsAny): JsAny? =
    Reflect.get(constructor, "prototype")

internal fun constructorOf(instance: JsAny?): JsAny? =
    instance?.let { Reflect.get(it, "constructor") }

internal fun ownEnumerableKeys(target: JsAny): ReadonlyArray<String> =
    // `Object.keys` is typed with `JsString` elements; those values are plain JS strings.
    Object.keys(target).unsafeCast<ReadonlyArray<String>>()

internal fun ownPropertyNames(target: JsAny): ReadonlyArray<String> =
    Object.getOwnPropertyNames(target).unsafeCast<ReadonlyArray<String>>()

internal fun parentPrototypeOf(prototype: JsAny?): JsAny? =
    prototype?.let { Reflect.getPrototypeOf(it) }

internal fun isFunction(value: JsAny?): Boolean =
    jsTypeOf(value) == "function"

internal fun isObject(value: JsAny?): Boolean =
    value != null && jsTypeOf(value) == "object"

/**
 * Whether `prototype.name` is a method or an accessor, i.e. something MobX can annotate as an action
 * or a computed value. A plain data property on the prototype is not one.
 */
internal fun isMethodProperty(prototype: JsAny, name: String): Boolean {
    val descriptor = Object.getOwnPropertyDescriptor<JsAny?>(prototype, name) ?: return false
    return jsTypeOf(descriptor.value) == "function" || jsTypeOf(descriptor.get) == "function"
}

internal fun readProperty(target: JsAny?, name: String): JsAny? =
    target?.let { Reflect.get(it, name) }

internal fun writeProperty(target: JsAny, name: String, value: JsAny?) {
    Reflect.set(target, name, value)
}

internal fun deleteProperty(target: JsAny, name: String): Boolean =
    Reflect.deleteProperty(target, name)

/** Reads a property keyed by a symbol, i.e. one that never shows up in enumeration. */
internal fun readSymbolProperty(target: JsAny?, key: JsAny?): JsAny? =
    if (target == null || key == null) null else Reflect.get(target, key.unsafeCast<PropertyKey>())

internal fun writeSymbolProperty(target: JsAny, key: JsAny, value: JsAny?) {
    Reflect.set(target, key.unsafeCast<PropertyKey>(), value)
}

/**
 * Calls a JS function with an explicit receiver.
 *
 * `fn.call(receiver, ...args)` is not expressible in Kotlin: a Kotlin function type is invoked as
 * `fn(args)`, which uses `undefined` as the receiver. Every patched member is a function that reads
 * `this`, so the call has to go through `Function.prototype.apply` — kotlin-js types `Reflect` but
 * has no `Reflect.apply`, so this one stays a snippet.
 */
internal fun applyFunction(fn: JsAny, receiver: JsAny, args: ReadonlyArray<JsAny?>): JsAny? =
    js("fn.apply(receiver, args)")

/**
 * Builds the function that replaces a render method on the prototype.
 *
 * The wrapper has to be a real `function`, not a Kotlin lambda: it must see the receiver the caller
 * used (`cart.view()`), and Kotlin compiles lambdas to arrow functions, which capture the enclosing
 * `this` instead. A Kotlin function that reads `js("this")` would work but depends on the compiler
 * not inlining it; spelling the wrapper in JavaScript makes the semantics explicit.
 */
private val methodWrapperFactory: dynamic =
    js(
        """(function (original, island) {
            return function () {
                return island(this, original, Array.prototype.slice.call(arguments));
            };
        })""",
    )

internal fun wrapMethod(
    original: JsAny,
    island: (receiver: JsAny, original: JsAny, args: ReadonlyArray<JsAny?>) -> JsAny?,
): JsAny =
    methodWrapperFactory(original, island)
