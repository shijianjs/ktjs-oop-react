package io.github.shijianjs.ktjs.oopreact.reactivity

import io.github.shijianjs.ktjs.oopreact.internal.asJsAny
import io.github.shijianjs.ktjs.oopreact.reactivity.mobx.ObservableArray
import io.github.shijianjs.ktjs.oopreact.reactivity.mobx.isObservable
import io.github.shijianjs.ktjs.oopreact.reactivity.mobx.observable
import io.github.shijianjs.ktjs.oopreact.internal.castFrom

/**
 * A mutable list whose changes are observable, so `items.add(todo)` re-renders the islands that read
 * the list.
 *
 * ```kotlin
 * @OopReactState
 * class Todos {
 *     val items: MutableList<Todo> = OopReactList()
 *     fun add(title: String) { items.add(Todo(title)) }   // one notification
 * }
 * ```
 *
 * It exists because Kotlin's own `ArrayList` is a class instance: MobX only tracks arrays, plain
 * objects, maps and sets, so `mutableListOf(...).add(...)` would mutate a value nothing observes and
 * the UI would quietly stay behind. The list is backed by MobX's own observable array, so reads are
 * tracked element by element and writes notify once per mutation.
 *
 * The alternative — an immutable `List` plus reassignment — works with the plain field annotation and
 * is the idiomatic Kotlin choice when a whole new list is the natural unit of change. Reach for this
 * one when the list is the unit: adding, removing and reordering in place.
 */
class OopReactList<T : Any>(
    initial: List<T> = emptyList(),
) : AbstractMutableList<T>() {

    private val items: ObservableArray = observable.array(null).also { array ->
        for (element in initial) {
            array.push(element.asJsAny())
            observe(element)
        }
    }

    /**
     * Makes an element that is itself a state object observable.
     *
     * The list holds items whose fields the UI reads (`item.done`), so those fields have to be
     * tracked. Walking the graph from the object that renders it stops at this list — a collection is
     * not a state class — so the list adopts its elements on the way in.
     */
    private fun observe(element: T) {
        val value = element.asJsAny()
        if (metasFor(value).isEmpty()) return
        if (isObservable(value)) return
        ensureObservable(value)
    }

    override val size: Int
        get() = items.length

    override fun get(index: Int): T {
        checkIndex(index)
        return castFrom(items[index])
    }

    override fun set(index: Int, element: T): T {
        checkIndex(index)
        observe(element)
        val previous: T = castFrom(items[index])
        items[index] = element.asJsAny()
        return previous
    }

    override fun add(index: Int, element: T) {
        observe(element)
        if (index == size) {
            items.push(element.asJsAny())
            return
        }

        items.splice(index, 0, element.asJsAny())
    }

    override fun removeAt(index: Int): T {
        checkIndex(index)
        return castFrom(items.splice(index, 1)[0])
    }

    override fun clear() {
        if (items.length == 0) return
        items.splice(0, items.length)
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("index: $index, size: $size")
        }
    }
}
