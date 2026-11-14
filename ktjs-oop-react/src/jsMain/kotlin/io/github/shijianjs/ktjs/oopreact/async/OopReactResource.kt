package io.github.shijianjs.ktjs.oopreact.async

import io.github.shijianjs.ktjs.oopreact.reactivity.oopReactObservable

/**
 * 一份远程数据：`data` / `loading` / `error`，加上把它取回来的 [load]。
 *
 * ```kotlin
 * class OrdersPage {
 *     val orders = OopReactResource<List<Order>>()
 *
 *     suspend fun open() {
 *         orders.load { api.orders() }
 *     }
 *
 *     @OopReactRender
 *     @JsName("table")
 *     fun table(): ReactNode = OopReactAsyncView(orders, skeleton = { TableSkeleton() }, ready = { … })
 * }
 * ```
 *
 * 没有缓存、没有去重、没有 stale-while-revalidate —— 那一层策略一进来就得跟着框架走。
 * 这里只回答「这份数据现在是什么」；要重新取，就再调一次 [load] 把同一个 fetcher 传进来。
 *
 * 失败时旧数据不丢：[OopReactAsyncView] 会把错误显示在内容上方。要清空，用 [invalidate]。
 *
 * 它自己调用 `oopReactObservable`：库自带的类不需要注解，也就不会被消费者模块里的 KSP 漏掉。
 */
class OopReactResource<T : Any> : OopReactAsyncState<T> {
    val loading: OopReactLoading = OopReactLoading()

    override var data: T? = null

    override var error: Throwable? = null

    init {
        oopReactObservable(this)
    }

    suspend fun load(
        timeoutMs: Long = 0,
        fetcher: suspend () -> T,
    ): OopReactTaskResult<T> {
        val result = loading.run(timeoutMs, fetcher)
        apply(result)
        return result
    }

    /** 清空数据，让读它的地方回到「还没有」。 */
    fun invalidate() {
        data = null
        error = null
    }

    /** `await` 之后的几次写入收在一个方法里，仍然只通知一次。 */
    private fun apply(result: OopReactTaskResult<T>) {
        when (result) {
            is OopReactTaskResult.Success -> {
                data = result.value
                error = null
            }

            is OopReactTaskResult.Failure -> {
                error = result.error
            }
        }
    }
}
