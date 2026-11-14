package io.github.shijianjs.ktjs.oopreact.reactivity

import io.github.shijianjs.ktjs.oopreact.internal.isObject
import io.github.shijianjs.ktjs.oopreact.internal.ownEnumerableKeys
import io.github.shijianjs.ktjs.oopreact.internal.readProperty
import io.github.shijianjs.ktjs.oopreact.internal.ownPropertyNames
import io.github.shijianjs.ktjs.oopreact.internal.parentPrototypeOf
import io.github.shijianjs.ktjs.oopreact.internal.prototypeOf
import io.github.shijianjs.ktjs.oopreact.internal.constructorOf
import io.github.shijianjs.ktjs.oopreact.internal.isMethodProperty
import io.github.shijianjs.ktjs.oopreact.internal.reportDuplicateMobx
import io.github.shijianjs.ktjs.oopreact.internal.writeProperty
import io.github.shijianjs.ktjs.oopreact.internal.AUTO_ANNOTATION
import io.github.shijianjs.ktjs.oopreact.reactivity.mobx.isObservable
import io.github.shijianjs.ktjs.oopreact.reactivity.mobx.makeObservable
import io.github.shijianjs.ktjs.oopreact.reactivity.mobx.makeObservableWithoutOptions
import js.reflect.unsafeCast
import kotlin.js.JsAny

/**
 * Declares an object's state, from the object itself:
 *
 * ```kotlin
 * @OopReactState
 * class Cart {
 *     val items = OopReactList<Item>()
 *
 *     init {
 *         oopReactObservable(this)   // only needed for eager observability
 *     }
 * }
 * ```
 *
 * Every field becomes observable, every method an action bound to the instance. On top of MobX's
 * `makeAutoObservable`:
 *
 * - **Inheritance.** The whole prototype chain is annotated, and calling this again in a subclass
 *   constructor only adds the keys that appeared since. `makeAutoObservable` refuses a class with a
 *   superclass, which is why this walks the chain itself.
 * - **A denylist instead of a whitelist.** Fields that are not state are marked with
 *   `@OopReactIgnore` beside their declaration instead of being collected in one map at the
 *   constructor. The `overrides` map still works and wins.
 *
 * `@OopReactState` classes get this for free before their first render; calling it by hand is how an
 * object becomes observable *before* it is rendered at all — the one thing the lazy path cannot do.
 *
 * @param overrides per-field MobX decisions keyed by **JavaScript** name (`@JsName` value): `true`
 *   means "pick the annotation automatically" (the default this function already applies), `false`
 *   means "leave this field alone". Anything more specific belongs in MobX's own API, which the
 *   application can call directly.
 * @param autoBind bind methods to the instance, so `this.onClick` is a stable reference.
 * @param deep track nested objects and arrays, or only reassignment of the field itself.
 */
fun <T : Any> oopReactObservable(
    instance: T,
    overrides: Map<String, Boolean> = emptyMap(),
    autoBind: Boolean = true,
    deep: Boolean = true,
): T {
    reportDuplicateMobx()

    val target: JsAny = unsafeCast(instance)
    if (isObservable(target)) return instance

    val ignored = ignoredNames(target) - overrides.filterValues { it }.keys
    val renders = renderMetas(target).keys
    val annotations = buildAnnotations(target, ignored, renders, overrides)
    if (annotations.isEmpty()) return instance

    val map: JsAny = js("({})")
    for ((name, annotation) in annotations) {
        writeProperty(map, name, annotation)
    }

    makeObservable(target, map, js("({autoBind: autoBind, deep: deep})"))
    makeNestedStateObservable(target)

    return instance
}

/**
 * Annotates an instance that a render method is about to use, when its class is `@OopReactState`.
 *
 * Called by the island machinery, never by application code: it is what makes `@OopReactState` mean
 * "state" without a constructor call.
 */
internal fun ensureObservable(instance: JsAny) {
    if (!isStateInstance(instance)) return
    if (isObservable(instance)) return
    oopReactObservableFor(instance)
}

private fun oopReactObservableFor(instance: JsAny) {
    reportDuplicateMobx()

    val ignored = ignoredNames(instance)
    val renders = renderMetas(instance).keys
    val annotations = buildAnnotations(instance, ignored, renders, emptyMap())
    if (annotations.isEmpty()) return

    val map: JsAny = js("({})")
    for ((name, annotation) in annotations) {
        writeProperty(map, name, annotation)
    }
    makeObservableWithoutOptions(target = instance, annotations = map)
    makeNestedStateObservable(instance)
}

/**
 * Makes the state objects this one holds observable too.
 *
 * Kotlin has no constructor hook, so `@OopReactState` cannot do what the TypeScript decorator does —
 * wrap the constructor and annotate every instance as it is created. Instead the graph is walked once,
 * from the object that is rendered: its fields are already observable, and each value that belongs to
 * a registered state class is made observable in turn, recursively.
 *
 * Without this step a plain field like `val settings = Settings()` would never be observable: nothing
 * renders it as an island, so nothing would ever ask. A cycle terminates on the first check, because
 * an object is observable before its fields are walked.
 */
private fun makeNestedStateObservable(instance: JsAny) {
    val own = ownEnumerableKeys(instance)
    for (index in 0 until own.size) {
        val value = readProperty(instance, own[index]) ?: continue
        if (!isObject(value)) continue
        if (metasFor(value).isEmpty()) continue
        if (isObservable(value)) continue
        ensureObservable(value)
    }
}

/**
 * The key set to hand MobX, all with the "auto" annotation.
 *
 * `true` is what makes this work: MobX's auto annotation inspects the descriptor and picks
 * observable / computed / bound action on its own, honouring the `deep` and `autoBind` options.
 * Passing it explicitly is what lets us hand over a key set that `makeAutoObservable` would refuse to
 * build.
 */
private fun buildAnnotations(
    target: JsAny,
    ignored: Set<String>,
    renders: Set<String>,
    overrides: Map<String, Boolean>,
): Map<String, JsAny> {
    val result = LinkedHashMap<String, JsAny>()

    fun put(name: String) {
        if (name in ignored || name in renders) return
        if (overrides[name] == false) return
        result[name] = AUTO_ANNOTATION
    }

    // Own fields — including a function held in a field (`val save = { … }`), which the auto
    // annotation treats as a method.
    val own = ownEnumerableKeys(target)
    for (index in 0 until own.size) {
        put(own[index])
    }

    // Methods and getters, own **and inherited**. This is the part `makeAutoObservable` cannot do:
    // it throws on a class with a superclass.
    var prototype = prototypeOf(constructorOf(target) ?: return result)
    while (prototype != null) {
        val names = ownPropertyNames(prototype)
        for (index in 0 until names.size) {
            val name = names[index]
            if (name == "constructor") continue
            if (isMethodProperty(prototype, name)) put(name)
        }
        prototype = parentPrototypeOf(prototype)
    }

    // `@OopReactAction` wins over everything above: the method is one transaction even if the map
    // (or `autoBind = false`) would have left it plain.
    for (name in actionNames(target)) {
        if (isMethodPropertyOf(target, name)) result[name] = AUTO_ANNOTATION
    }

    return result
}

private fun isMethodPropertyOf(target: JsAny, name: String): Boolean {
    var prototype = prototypeOf(constructorOf(target) ?: return false)
    while (prototype != null) {
        if (isMethodProperty(prototype, name)) return true
        prototype = parentPrototypeOf(prototype)
    }
    return false
}
