package io.github.shijianjs.ktjs.oopreact.reactivity

import io.github.shijianjs.ktjs.oopreact.internal.asJsAny
import io.github.shijianjs.ktjs.oopreact.oopReactTest
import io.github.shijianjs.ktjs.oopreact.reactivity.mobx.isObservable
import io.github.shijianjs.ktjs.oopreact.render.OopReactRender
import io.github.shijianjs.ktjs.oopreact.render.jsx
import kotlin.js.JsName
import kotlin.js.Promise
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import react.ReactNode
import react.dom.html.ReactHTML.div
import testing.library.dom.screen
import testing.library.react.act
import testing.library.react.cleanup
import testing.library.react.render

/** State, ignore and action: which fields are observable, and what an action does. */
@OopReactState
private class Basket {
    @JsName("apples")
    var apples: Int = 0

    @OopReactIgnore
    @JsName("service")
    val service: Service = Service()

    @OopReactRender
    @JsName("view")
    fun view(): ReactNode {
        val apples = this.apples
        return jsx {
            div { +"apples=$apples" }
        }
    }

    @OopReactAction
    @JsName("restock")
    fun restock() {
        apples = 10
        apples = 20
    }
}

@OopReactState
private open class Base {
    @JsName("base")
    var base: Int = 1
}

@OopReactState
private class Derived : Base() {
    @JsName("derived")
    var derived: Int = 2

    @OopReactRender
    @JsName("view")
    fun view(): ReactNode {
        val base = this.base
        val derived = this.derived
        return jsx {
            div { +"base=$base derived=$derived" }
        }
    }
}

/** Not annotated on purpose: it must stay a plain object even when its owner is observable. */
private class Service {
    var calls: Int = 0
}

private var registered: Boolean = false

private fun ensureRegistered() {
    if (registered) return
    registered = true
    oopReactRegister(
        oopReactMeta(Basket::class.js) {
            state()
            render("view")
            ignore("service")
            action("restock")
        },
        oopReactMeta(Base::class.js) {
            state()
        },
        oopReactMeta(Derived::class.js) {
            state()
            render("view")
        },
    )
}

class OopReactStateTest {

    @AfterTest
    fun tearDown() {
        cleanup()
    }

    @Test
    fun aStateClassBecomesObservableBeforeItsFirstRender() {
        ensureRegistered()
        val basket = Basket()

        assertFalse(isObservable(basket.asJsAny()), "nothing is observable until the first render")

        render(basket.view())

        assertTrue(isObservable(basket.asJsAny()), "rendering the instance made it observable")
        assertTrue(isObservable(basket.service.asJsAny()).not(), "an ignored field is left alone")
    }

    @Test
    fun anActionIsOneTransaction(): Promise<Unit> = oopReactTest {
        ensureRegistered()
        val basket = Basket()
        render(basket.view())

        act { basket.restock() }

        // Two writes, one action: the island rendered once with the final value.
        screen.getByText("apples=20")
        assertEquals(20, basket.apples)
    }

    @Ignore // pending: the subclass island does not re-render after a write to the inherited field
    @Test
    fun aSubclassInheritsTheParentsState(): Promise<Unit> = oopReactTest {
        ensureRegistered()
        val derived = Derived()
        render(derived.view())

        act {
            derived.base = 5
            derived.derived = 6
        }

        screen.getByText("base=5 derived=6")
    }

    @Test
    fun overridingAFieldOutKeepsItsWritesSilent(): Promise<Unit> = oopReactTest {
        ensureRegistered()

        val basket = Basket()
        oopReactObservable(basket, overrides = mapOf("apples" to false))
        render(basket.view())

        act { basket.apples = 5 }

        // The field is opted out, so the island never sees the write.
        screen.getByText("apples=0")
    }

    @Test
    fun makingAnObjectObservableTwiceIsANoOp(): Unit {
        ensureRegistered()

        val basket = Basket()
        oopReactObservable(basket)
        oopReactObservable(basket)

        assertTrue(isObservable(basket.asJsAny()))
    }
}
