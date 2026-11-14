package io.github.shijianjs.ktjs.oopreact.internal

import js.array.ReadonlyArray
import kotlin.js.JsAny

/**
 * The handful of JavaScript operations this library would otherwise spell as one-off `js(...)`
 * snippets.
 *
 * They are collected here, not spread over the call sites, because each of them is a place where
 * Kotlin's own semantics differ from JavaScript's and the difference has to be visible in one
 * comment instead of ten.
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
    jsAnyOf(this)

/** The other direction: a JS value used where Kotlin expects `Any`. */
internal fun <T> JsAny?.asAny(): T =
    castFrom(this)

private fun jsAnyOf(value: Any?): JsAny =
    js("value")

internal fun prototypeOf(constructor: JsAny): JsAny? =
    js("constructor.prototype")

internal fun constructorOf(instance: JsAny?): JsAny? =
    js("instance !== null && instance !== undefined ? instance.constructor : undefined")

internal fun ownEnumerableKeys(target: JsAny): ReadonlyArray<String> =
    js("Object.keys(target)")

internal fun ownPropertyNames(target: JsAny): ReadonlyArray<String> =
    js("Object.getOwnPropertyNames(target)")

internal fun parentPrototypeOf(prototype: JsAny?): JsAny? =
    js("prototype === null || prototype === undefined ? undefined : Object.getPrototypeOf(prototype)")

internal fun isFunction(value: JsAny?): Boolean =
    js("typeof value === 'function'")

internal fun isObject(value: JsAny?): Boolean =
    js("value !== null && typeof value === 'object'")

/**
 * Whether `prototype.name` is a method or an accessor, i.e. something MobX can annotate as an action
 * or a computed value. A plain data property on the prototype is not one.
 */
internal fun isMethodProperty(prototype: JsAny, name: String): Boolean =
    js(
        """(function (descriptor) {
            return descriptor !== undefined
                && (typeof descriptor.value === 'function' || typeof descriptor.get === 'function');
        })(Object.getOwnPropertyDescriptor(prototype, name))""",
    )

internal fun readProperty(target: JsAny?, name: String): JsAny? =
    js("target === null || target === undefined ? undefined : target[name]")

internal fun writeProperty(target: JsAny, name: String, value: JsAny?) {
    js("target[name] = value")
}

internal fun deleteProperty(target: JsAny, name: String): Boolean =
    js("delete target[name]")

/** Reads a property keyed by a symbol, i.e. one that never shows up in enumeration. */
internal fun readSymbolProperty(target: JsAny?, key: JsAny?): JsAny? =
    js("target === null || target === undefined ? undefined : target[key]")

internal fun writeSymbolProperty(target: JsAny, key: JsAny, value: JsAny?) {
    js("target[key] = value")
}

/**
 * Calls a JS function with an explicit receiver.
 *
 * `fn.call(receiver, ...args)` is not expressible in Kotlin: a Kotlin function type is invoked as
 * `fn(args)`, which uses `undefined` as the receiver. Every patched member is a function that reads
 * `this`, so the call has to go through `Function.prototype.apply`.
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
