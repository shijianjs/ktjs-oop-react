package io.github.shijianjs.ktjs.oopreact

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * Runs a test body that has to `await` something — an `act` around a state write, a promise from a
 * task — as a Kotlin/JS test.
 *
 * kotlin.test on JS awaits a returned `Promise`, so a test that needs to yield declares
 * `fun name(): Promise<Unit> = oopReactTest { … }` instead of using `runTest` and a fake clock: the
 * thing under test is React's own scheduling, which a test scheduler would replace with a lie.
 */
internal fun oopReactTest(block: suspend CoroutineScope.() -> Unit): Promise<Unit> =
    CoroutineScope(Dispatchers.Default).promise { block() }
