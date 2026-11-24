package demo.pages

import demo.app.RenderStats
import demo.app.Section
import demo.app.Settings
import demo.i18n.Msg
import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState
import io.github.shijianjs.ktjs.oopreact.render.OopReactRender
import io.github.shijianjs.ktjs.oopreact.render.jsx
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Stack
import mui.material.StackDirection
import mui.material.Typography
import mui.material.styles.TypographyVariant
import mui.system.responsive
import react.ReactNode
import web.cssom.ClassName

/**
 * Three islands of one object.
 *
 * Each `@OopReactRender` method is its own island: it subscribes to the fields *it* reads. Clicking
 * one counter re-runs that method only — every counter prints how many times it has run, so the
 * isolation is visible rather than asserted.
 */
@OopReactState
class CounterPage(
    private val settings: Settings,
) : Page {
    private val left: Counter = Counter("left")

    private val middle: Counter = Counter("middle")

    private val right: Counter = Counter("right")

    override fun content(): ReactNode =
        jsx {
            Section(settings.t(Msg.counterTitle), settings.t(Msg.counterHint)) {
                Stack {
                    direction = responsive(StackDirection.row)
                    className = ClassName("app-row")
                    +left.widget()
                    +middle.widget()
                    +right.widget()
                }
            }
        }

    /** One counter: the value, its render count, and three buttons. */
    @OopReactState
    class Counter(
        private val label: String,
    ) {
        var value: Int = 0

        @OopReactRender
        @JsName("widget")
        fun widget(): ReactNode {
            val runs = RenderStats.bump("counter.$label")
            val current = value
            return jsx {
                Stack {
                    Typography {
                        variant = TypographyVariant.h5
                        +"$label: $current"
                    }
                    Typography {
                        variant = TypographyVariant.caption
                        className = ClassName("app-caption")
                        +"rendered $runs×"
                    }
                    Stack {
                        direction = responsive(StackDirection.row)
                        className = ClassName("app-row")
                        Button {
                            variant = ButtonVariant.outlined
                            onClick = { this@Counter.value = current - 1 }
                            +"−"
                        }
                        Button {
                            variant = ButtonVariant.contained
                            onClick = { this@Counter.value = current + 1 }
                            +"+"
                        }
                        Button {
                            variant = ButtonVariant.text
                            onClick = { this@Counter.value = 0 }
                            +"0"
                        }
                    }
                }
            }
        }
    }
}
