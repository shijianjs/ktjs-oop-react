package io.github.shijianjs.ktjs.oopreact.internal

import js.symbol.Symbol
import kotlin.js.JsAny

/**
 * Where `oopReactComponent` parks the name it was given, so an island can report it without the
 * runtime keeping a registry: a symbol, so nothing enumerates it and MobX never sees it as state.
 */
private val COMPONENT_NAME: JsAny = Symbol("oop-react.componentName")

internal fun setComponentName(instance: JsAny, name: String) {
    writeSymbolProperty(instance, COMPONENT_NAME, name)
}

/**
 * The name a component was mounted under, if any.
 *
 * It is read when an island is created rather than when the class is registered: a production build
 * minifies class names, and the name a reader should see is the one the component was given.
 */
internal fun componentName(instance: JsAny): String? =
    readSymbolProperty(instance, COMPONENT_NAME) as? String
