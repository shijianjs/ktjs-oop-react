package io.github.shijianjs.ktjs.oopreact.async

import react.ReactNode

/** 任何知道「现在的值」和「上次取数的错误」的对象；[OopReactResource] 是其中一个，字段相同的也算。 */
interface OopReactAsyncState<out T> {
    val data: T?

    val error: Throwable?
}

/**
 * 延迟数据的三种画面，一次调用全给出：骨架 / 内容 / 失败。
 *
 * ```kotlin
 * @OopReactRender
 * @JsName("view")
 * fun view(): ReactNode = OopReactAsyncView(
 *     rows,
 *     skeleton = { Skeleton() },
 *     ready = { table() },
 *     failed = { error -> Alert { +describe(error) } },
 * )
 * ```
 *
 * 它是普通函数而不是组件：在渲染方法里调用时，对 `data` / `error` 的读取属于**那个岛**，
 * 所以不需要额外的 observer —— 这也是它能填进任何「只放一个节点」的位置的原因（对话框的 children、
 * 表格单元格、渲染 prop 的返回值）。
 *
 * 失败不清掉旧数据：值留在屏幕上，错误显示在它上方。清空是 [OopReactResource.invalidate] 的事。
 */
fun <T> OopReactAsyncView(
    resource: OopReactAsyncState<T>,
    skeleton: () -> ReactNode?,
    ready: () -> ReactNode?,
    failed: ((Throwable) -> ReactNode?)? = null,
): ReactNode? =
    oopReactAsyncView(
        data = resource.data,
        error = resource.error,
        skeleton = skeleton,
        ready = ready,
        failed = failed,
    )

/** [OopReactAsyncView] 的另一种入口：手上有 `data` / `error` 两个字段，但不是 [OopReactAsyncState]。 */
fun OopReactAsyncView(
    data: Any?,
    error: Throwable?,
    skeleton: () -> ReactNode?,
    ready: () -> ReactNode?,
    failed: ((Throwable) -> ReactNode?)? = null,
): ReactNode? =
    oopReactAsyncView(
        data = data,
        error = error,
        skeleton = skeleton,
        ready = ready,
        failed = failed,
    )

private fun oopReactAsyncView(
    data: Any?,
    error: Throwable?,
    skeleton: () -> ReactNode?,
    ready: () -> ReactNode?,
    failed: ((Throwable) -> ReactNode?)?,
): ReactNode? {
    val failure = if (error == null) null else failed?.invoke(error)

    if (data != null) {
        if (failure == null) return ready()
        return ReactNode(arrayOf(failure, ready()))
    }

    // No data: a failure shows only the failure — without a renderer nothing is shown at all, rather
    // than pretending it is still loading.
    return if (error == null) skeleton() else failure
}
