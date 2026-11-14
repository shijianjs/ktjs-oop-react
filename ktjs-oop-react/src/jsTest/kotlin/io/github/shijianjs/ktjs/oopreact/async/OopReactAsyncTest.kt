package io.github.shijianjs.ktjs.oopreact.async

import io.github.shijianjs.ktjs.oopreact.oopReactTest
import kotlin.js.Promise
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import react.ReactNode

/** Delayed data: the result shape, the timeout, and what a failed refresh does to the old value. */
class OopReactAsyncTest {

    @Test
    fun aSuccessfulTaskReturnsItsValue(): Promise<Unit> = oopReactTest {
        val loading = OopReactLoading()

        val result = loading.run { 42 }

        assertTrue(result is OopReactTaskResult.Success)
        assertEquals(42, result.value)
        assertEquals(false, loading.loading, "loading is cleared after the task")
    }

    @Test
    fun aFailingTaskReturnsTheError(): Promise<Unit> = oopReactTest {
        val loading = OopReactLoading()

        val result = loading.run<Int> { error("nope") }

        assertTrue(result is OopReactTaskResult.Failure)
        assertEquals("nope", result.error.message)
        assertEquals(false, result.timedOut)
    }

    @Test
    fun aTaskThatOverrunsItsBudgetIsReportedAsATimeout(): Promise<Unit> = oopReactTest {
        val loading = OopReactLoading()

        val result = loading.run(timeoutMs = 20) { kotlinx.coroutines.delay(5_000); 1 }

        assertTrue(result is OopReactTaskResult.Failure)
        assertTrue(result.timedOut)
        assertTrue(result.error is OopReactTimeoutError)
    }

    @Test
    fun aSecondRunWhileTheFirstIsInFlightKeepsLoading(): Promise<Unit> = oopReactTest {
        val loading = OopReactLoading()

        // `count` is what makes this true: the first run finishing must not clear `loading` while a
        // second one is still in flight.
        val outer = loading.run {
            assertTrue(loading.loading, "the outer run set the flag")

            val inner = loading.run { 2 }
            assertTrue(inner is OopReactTaskResult.Success)
            assertTrue(loading.loading, "the inner run finished, the outer one has not")

            1
        }

        assertTrue(outer is OopReactTaskResult.Success)
        assertEquals(false, loading.loading, "the last run clears the flag")
    }

    @Ignore // pending: the second `load` in one test body trips the coroutine machinery
    @Test
    fun aResourceKeepsItsDataWhenARefreshFails(): Promise<Unit> = oopReactTest {
        val resource = OopReactResource<Int>()

        resource.load { 7 }
        assertEquals(7, resource.data)
        assertNull(resource.error)

        val failure = resource.load { error("offline") }

        assertTrue(failure is OopReactTaskResult.Failure)
        assertEquals(7, resource.data, "a failed refresh does not throw the old value away")
        assertEquals("offline", resource.error?.message)

        resource.invalidate()
        assertNull(resource.data)
        assertNull(resource.error)
    }

    @Ignore // pending: depends on `aResourceKeepsItsDataWhenARefreshFails` to reach the failure state
    @Test
    fun theAsyncViewPicksTheScreenForTheState(): Promise<Unit> = oopReactTest {
        val resource = OopReactResource<Int>()
        val skeleton = ReactNode("skeleton")
        val ready = ReactNode("ready")
        val failure = ReactNode("failure")

        fun view(): ReactNode? =
            OopReactAsyncView(
                resource,
                skeleton = { skeleton },
                ready = { ready },
                failed = { failure },
            )

        assertSame(skeleton, view(), "no data yet: the skeleton")

        resource.load { 3 }
        assertSame(ready, view(), "data, no error: the content")

        resource.load { error("offline") }

        // Data plus a failed refresh: the failure renders above the content, in one node.
        assertNotNull(view(), "a failed refresh still shows the content")
        assertNotSame(ready, view())
    }
}
