package demo.pages

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
import mui.system.responsive
import react.ReactNode
import web.cssom.ClassName

/**
 * One broken island and one healthy one, in the same object.
 *
 * The error boundary wraps each island, so [broken] renders the fallback in its own place while
 * [healthy] — rendered by the same object, in the same tree — keeps working. `broken` throws on
 * purpose: its fallback's **Retry** re-runs the method and throws again, which is the honest
 * behaviour. Press "repair" first and the retry succeeds, because the flag it reads changes.
 */
@OopReactState
class FailurePage(
    private val settings: Settings,
) : Page {
    override val path: String = "failure"

    override val title: Msg = Msg.navFailure

    var shouldThrow: Boolean = true

    override fun content(): ReactNode = jsx {
        Section(settings.t(Msg.failureTitle), settings.t(Msg.failureHint)) {
            +healthy()
            +broken()
        }
    }

    fun repair() {
        shouldThrow = false
    }

    @OopReactRender
    @JsName("healthy")
    fun healthy(): ReactNode {
        val settings = this.settings
        val shouldThrow = this.shouldThrow
        return jsx {
            Stack {
                direction = responsive(StackDirection.row)
                className = ClassName("app-row")
                Typography { +settings.t(Msg.healthy) }
                Button {
                    variant = ButtonVariant.outlined
                    disabled = shouldThrow
                    onClick = { repair() }
                    +settings.t(Msg.throwError)
                }
            }
        }
    }

    @OopReactRender
    @JsName("broken")
    fun broken(): ReactNode {
        if (shouldThrow) {
            error("FailurePage.broken() throws on purpose: this is what one island failing looks like.")
        }

        val settings = this.settings
        return jsx {
            Typography {
                className = ClassName("app-muted")
                +(settings.t(Msg.brokenOnPurpose) + " / repaired")
            }
        }
    }
}
