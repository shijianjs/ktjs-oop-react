package io.github.shijianjs.ktjs.oopreact.render

import io.github.shijianjs.ktjs.oopreact.oopReactTest
import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState
import io.github.shijianjs.ktjs.oopreact.reactivity.oopReactObservable
import io.github.shijianjs.ktjs.oopreact.reactivity.oopReactMeta
import io.github.shijianjs.ktjs.oopreact.reactivity.oopReactRegister
import kotlin.js.JsName
import kotlin.js.Promise
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import react.Key
import react.ReactNode
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import testing.library.dom.screen
import testing.library.react.act
import testing.library.react.cleanup
import testing.library.react.render

/** Islands without a class method (observer, callback) and the memo that keeps them independent. */
class OopReactObserverTest {

    @AfterTest
    fun tearDown() {
        cleanup()
    }

    @Test
    fun theObserverTracksWhateverItsBlockReads(): Promise<Unit> = oopReactTest {
        val model = oopReactObservable(Model())
        render(OopReactObserver { jsx { +"count=${model.count}" } })

        act { model.count = 3 }

        screen.getByText("count=3")
    }

    @Test
    fun aKeyedCallbackRendersOneIslandPerEntry(): Promise<Unit> = oopReactTest {
        val model = oopReactObservable(Model(count = 1))
        val renderRow = OopReactCallback<Int>(key = { Key(it) }) { value ->
            jsx { +"row=${value + model.count}" }
        }

        render(
            jsx {
                ul {
                    for (value in 1..2) {
                        li {
                            key = Key(value)
                            +renderRow(value)
                        }
                    }
                }
            },
        )

        screen.getByText("row=2")
        screen.getByText("row=3")

        act { model.count = 10 }

        screen.getByText("row=11")
        screen.getByText("row=12")
    }

    @Ignore // pending: the host island does not re-render after a write to the field it read
    @Test
    fun anIslandIsSkippedWhenItsArgumentsDidNotChange(): Promise<Unit> = oopReactTest {
        Host.register()

        val counter = ChildCounter()
        val host = Host(counter)

        render(host.view())

        // The host island re-renders; the child island it creates has the same argument, so the
        // memo skips it and the counter's render method does not run again.
        act { host.label = "changed" }

        screen.getByText("label=changed")
        assertEquals(1, RenderTally.count("child"), "the child island ran once")
    }
}

/** Plain object: counting renders is a side effect, and a render method must not write state. */
private object RenderTally {
    private val counts: MutableMap<String, Int> = mutableMapOf()

    fun bump(name: String): Int {
        val next = (counts[name] ?: 0) + 1
        counts[name] = next
        return next
    }

    fun count(name: String): Int = counts[name] ?: 0
}

@OopReactState
private class Model(
    var count: Int = 0,
)

@OopReactState
private class ChildCounter {
    @OopReactRender
    @JsName("child")
    fun child(tag: String): ReactNode {
        RenderTally.bump("child")
        return jsx { +tag }
    }
}

@OopReactState
private class Host(
    private val counter: ChildCounter,
) {
    @JsName("label")
    var label: String = "initial"

    @OopReactRender
    @JsName("view")
    fun view(): ReactNode {
        val label = this.label
        return jsx {
            +"label=$label"
            +counter.child("child")
        }
    }

    companion object {
        private var registered: Boolean = false

        fun register() {
            if (registered) return
            registered = true
            oopReactRegister(
                oopReactMeta(ChildCounter::class.js, name = "Counter") {
                    state()
                    render("child")
                },
                oopReactMeta(Host::class.js, name = "Host") {
                    state()
                    render("view")
                },
            )
        }
    }
}
