package io.github.shijianjs.ktjs.oopreact.async

import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState
import io.github.shijianjs.ktjs.oopreact.reactivity.oopReactObservable
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The result of one asynchronous task: a value on success, an error on failure, and whether the
 * attempt ran out of time (the message a UI shows is usually different).
 */
sealed interface OopReactTaskResult<out T> {
    class Success<T>(
        val value: T,
    ) : OopReactTaskResult<T>

    class Failure(
        val error: Throwable,
        val timedOut: Boolean,
    ) : OopReactTaskResult<Nothing>
}

/** The error a timed-out run reports. `result.timedOut` and `is` both recognise it. */
class OopReactTimeoutError(
    val timeoutMs: Long,
) : Exception("Timed out after ${timeoutMs}ms") {
    override fun toString(): String = "OopReactTimeoutError: Timed out after ${timeoutMs}ms"
}

/**
 * One asynchronous operation's state: one per object (usually one button, one submit), independent of
 * every other.
 *
 * ```kotlin
 * class SaveButton {
 *     private val loading = OopReactLoading()
 *
 *     suspend fun click() {
 *         val result = loading.run(timeoutMs = 5_000) { api.save(order) }
 *         if (result is OopReactTaskResult.Failure) page.feedback.error(result.error)
 *     }
 *
 *     @OopReactRender
 *     @JsName("view")
 *     fun view(): ReactNode = Button {
 *         disabled = loading.loading
 *         onClick = { scope.launch { click() } }
 *         +"Save"
 *     }
 * }
 * ```
 *
 * It does exactly three things: raise [loading] before the task, always lower it afterwards, and turn
 * errors and timeouts into a return value. **No toasts, no logging, no progress bar** — that is the
 * business of whoever holds it.
 *
 * It keeps a count, so two overlapping runs on one object do not clear [loading] when the first one
 * ends.
 *
 * One difference from the TypeScript original: a timeout **cancels** the task (Kotlin coroutines can),
 * where a JS `Promise` keeps running. Pass an `AbortSignal` if a request has to be cancelled for real.
 *
 * The flag lives in a small state object rather than on this class: `run` is a coroutine, and an
 * observable method would be wrapped as a MobX action, which does not compose with `suspend`.
 */
class OopReactLoading {
    private val state: LoadingState = LoadingState()

    private var count: Int = 0

    val loading: Boolean
        get() = state.loading

    suspend fun <T> run(
        timeoutMs: Long = 0,
        task: suspend () -> T,
    ): OopReactTaskResult<T> {
        count += 1
        state.loading = true

        try {
            val value =
                if (timeoutMs > 0) {
                    val completed = withTimeoutOrNull(timeoutMs) { Box(task()) }
                    if (completed == null) {
                        return OopReactTaskResult.Failure(OopReactTimeoutError(timeoutMs), timedOut = true)
                    }
                    completed.value
                } else {
                    task()
                }

            return OopReactTaskResult.Success(value)
        } catch (error: Throwable) {
            return OopReactTaskResult.Failure(error, timedOut = false)
        } finally {
            count -= 1
            if (count == 0) state.loading = false
        }
    }
}

/** The observable bit of [OopReactLoading]; separate so no coroutine is ever annotated as an action. */
@OopReactState
private class LoadingState {
    var loading: Boolean = false

    init {
        oopReactObservable(this)
    }
}

/** Distinguishes "the task returned null" from "the task timed out". */
private class Box<T>(
    val value: T,
)
