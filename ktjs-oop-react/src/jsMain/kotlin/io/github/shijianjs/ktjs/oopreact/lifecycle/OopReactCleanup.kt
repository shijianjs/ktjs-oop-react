package io.github.shijianjs.ktjs.oopreact.lifecycle

/**
 * 一个对象攒下来的清理动作：`reaction`、timer、DOM 监听、第三方实例。
 *
 * ```kotlin
 * class Settings {
 *     val cleanup = OopReactCleanup()
 *
 *     init {
 *         cleanup.add(autorun { apply(theme) })
 *         cleanup.add { window.removeEventListener("hashchange", listener) }
 *     }
 *
 *     fun dispose() {
 *         cleanup.dispose()
 *     }
 * }
 * ```
 *
 * 普通类，没有状态、不参与渲染，所以不需要 `oopReactObservable`。
 */
class OopReactCleanup {
    private val disposers: MutableSet<() -> Unit> = mutableSetOf()

    private var disposed: Boolean = false

    val size: Int
        get() = disposers.size

    val isDisposed: Boolean
        get() = disposed

    /**
     * 登记一个清理动作，返回「撤销登记」的函数。
     * 在已经 [dispose] 过的对象上再登记，会**立刻执行** —— 漏清理比晚清理难查。
     */
    fun add(disposer: () -> Unit): () -> Unit {
        if (disposed) {
            disposer()
            return {}
        }

        disposers.add(disposer)
        return { disposers.remove(disposer) }
    }

    /**
     * 依次执行并清空。重复调用没有副作用。
     * 一个清理动作抛错不影响其余的：能清理多少算多少，这是收尾，不是事务。
     */
    fun dispose() {
        if (disposed) return

        disposed = true
        val pending = disposers.toList()
        disposers.clear()

        for (disposer in pending) {
            runCatching(disposer)
        }
    }
}
