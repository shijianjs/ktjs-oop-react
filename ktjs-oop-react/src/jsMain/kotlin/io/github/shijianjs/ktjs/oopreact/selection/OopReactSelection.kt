package io.github.shijianjs.ktjs.oopreact.selection

import io.github.shijianjs.ktjs.oopreact.reactivity.oopReactObservable

/**
 * 一组 id 的「选中了哪些」。
 *
 * ```kotlin
 * class OrdersPage {
 *     val selection = OopReactSelection<Int>()
 *
 *     @OopReactRender
 *     @JsName("table")
 *     fun table(): ReactNode = Table {
 *         rowSelection = RowSelection(
 *             selectedRowKeys = selection.ids,
 *             onChange = { keys -> selection.set(keys) },
 *         )
 *     }
 * }
 * ```
 *
 * 只存「选了什么」，不碰行数据：要判断哪些行选中了，行自己问 `selection.has(row.id)`。
 * 用 `List` 而不是 `Set`，因为一页几十行，按 id 比较比哈希更省事，也更好在 DevTools 里看；
 * 每次改动都换一个新的 `List`，所以普通的字段注解就能观察到它。
 */
class OopReactSelection<K>(
    initial: List<K> = emptyList(),
) {
    var ids: List<K> = initial

    init {
        oopReactObservable(this)
    }

    val size: Int
        get() = ids.size

    val isEmpty: Boolean
        get() = ids.isEmpty()

    fun has(id: K): Boolean = ids.contains(id)

    fun add(id: K) {
        if (ids.contains(id)) return
        ids = ids + id
    }

    fun remove(id: K) {
        ids = ids.filter { it != id }
    }

    fun toggle(id: K) {
        if (has(id)) remove(id) else add(id)
    }

    /** 整批替换（表格的 `onChange` 直接把它交进来）。 */
    fun set(values: List<K>) {
        ids = values.distinct()
    }

    fun clear() {
        ids = emptyList()
    }

    /** 全选：`total` 是这一批的总条数。空集合不算全选。 */
    fun isAll(total: Int): Boolean = total > 0 && ids.size >= total

    /** 半选：选了一些，但没选完 —— 复选框该画成横杠的那种状态。 */
    fun isPartial(total: Int): Boolean = ids.isNotEmpty() && ids.size < total
}
