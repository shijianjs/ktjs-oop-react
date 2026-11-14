package io.github.shijianjs.ktjs.oopreact.render

import io.github.shijianjs.ktjs.oopreact.oopReactTest
import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState
import io.github.shijianjs.ktjs.oopreact.reactivity.oopReactMeta
import io.github.shijianjs.ktjs.oopreact.reactivity.oopReactRegister
import kotlin.js.JsName
import kotlin.js.Promise
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import react.ReactNode
import react.dom.html.ReactHTML.div
import testing.library.dom.screen
import testing.library.react.act
import testing.library.react.cleanup
import testing.library.react.render

/** The smallest island: one observable field, one render method. */
@OopReactState
private class ClickCounter {
    var count: Int = 0

    @OopReactRender
    @JsName("view")
    fun view(): ReactNode =
        jsx {
            div {
                +"count=$count"
            }
        }
}

/** A private field is the interesting case: Kotlin/JS renames it, and MobX must still track it. */
@OopReactState
private class Secretive {
    private var hidden: Int = 0

    fun bump() {
        hidden += 1
    }

    @OopReactRender
    @JsName("view")
    fun view(): ReactNode {
        // A local, because `hidden` inside `div { … }` resolves to the HTML `hidden` attribute of the
        // element — the DSL receiver shadows the class. See the note in AGENTS.md.
        val hidden = this.hidden
        return jsx {
            div {
                +"hidden=$hidden"
            }
        }
    }
}

private var registered: Boolean = false

private fun ensureRegistered() {
    if (registered) return
    registered = true
    oopReactRegister(
        oopReactMeta(ClickCounter::class.js) {
            state()
            render("view")
        },
        oopReactMeta(Secretive::class.js) {
            state()
            render("view")
        },
    )
}

/**
 * The core contract, end to end: an annotated method returns an element, the element subscribes to
 * the fields the method reads, and a write re-renders only that island.
 */
class OopReactRenderTest {

    @AfterTest
    fun tearDown() {
        cleanup()
    }

    @Test
    fun theMethodReturnsAnElementThatRendersItsFields() {
        ensureRegistered()
        val counter = ClickCounter()

        render(counter.view())

        screen.getByText("count=0")
    }

    @Test
    fun writingAFieldReRendersTheIsland(): Promise<Unit> = oopReactTest {
        ensureRegistered()
        val counter = ClickCounter()
        render(counter.view())

        act {
            counter.count = 7
        }

        screen.getByText("count=7")
    }

    @Test
    fun aPrivateFieldIsTrackedToo(): Promise<Unit> = oopReactTest {
        ensureRegistered()
        val secretive = Secretive()
        render(secretive.view())

        act {
            secretive.bump()
        }

        screen.getByText("hidden=1")
    }

    @Test
    fun anUnregisteredClassIsNotPatched(): Unit {
        ensureRegistered()
        val node = Plain().view()

        assertTrue(node != null)
    }
}

/** A class with a render method but no metadata: nothing is patched, nothing is observable. */
private class Plain {
    @OopReactRender
    @JsName("view")
    fun view(): ReactNode =
        jsx {
            div { +"plain" }
        }
}
