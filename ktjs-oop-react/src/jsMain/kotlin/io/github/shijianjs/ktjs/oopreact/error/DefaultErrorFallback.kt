package io.github.shijianjs.ktjs.oopreact.error

import io.github.shijianjs.ktjs.oopreact.config.OopReactFallbackProps
import io.github.shijianjs.ktjs.oopreact.internal.castFrom
import kotlin.js.JsAny
import react.CSSProperties
import react.FC
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong
import web.aria.AriaRole
import web.cssom.ClassName
import web.html.ButtonType

/*
 * The fallback's few styles are written as JS object literals on purpose: `csstype` types every
 * declaration as a union of keywords and branded strings, which makes a five-property box longer than
 * the component it styles. The object is cast once, where it is declared, and never touched again.
 */
private val boxStyleSource: Any = js(
    """({border: '1px solid currentColor', borderRadius: '6px', color: 'inherit',
         display: 'flex', flexDirection: 'column', gap: '8px', padding: '8px 12px'})""",
)

private val messageStyleSource: Any = js(
    """({fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace', margin: 0,
         opacity: 0.75, overflowWrap: 'anywhere'})""",
)

private val retryStyleSource: Any = js("({alignSelf: 'flex-start'})")

private val boxStyle: CSSProperties = castFrom(boxStyleSource)

private val messageStyle: CSSProperties = castFrom(messageStyleSource)

private val retryStyle: CSSProperties = castFrom(retryStyleSource)

private val alertRole: AriaRole = castFrom("alert")

private val submitButtonType: ButtonType = castFrom("button")

/**
 * The fallback a failing render method lands on when the application has not configured its own with
 * `oopReactConfigure`.
 *
 * It is intentionally plain — a `div` with inline styles and no design-system dependency — because
 * the library must not drag a component kit into an application that never asked for one. An
 * application that already has a kit passes its own fallback; the demo does exactly that with MUI.
 */
val DefaultErrorFallback: FC<OopReactFallbackProps> =
    FC { props ->
        div {
            style = boxStyle
            role = alertRole
            className = ClassName("oop-react-fallback")

            strong {
                +if (props.name != null) "Render failed: ${props.name}" else "Render failed"
            }
            p {
                style = messageStyle
                +describe(props.error)
            }
            button {
                type = submitButtonType
                style = retryStyle
                onClick = { props.reset() }
                +"Retry"
            }
        }
    }

/** `Name: message` for anything thrown from a render; `String(error)` for everything else. */
private external interface ErrorLike {
    val name: String?
    val message: String?
}

private fun describe(error: JsAny?): String {
    if (error == null) return "Unknown error"

    val like: ErrorLike? = castFrom(error)
    val name = like?.name
    val message = like?.message
    if (name != null && message != null) return "$name: $message"

    val text: Any = js("String(error)")
    return castFrom(text)
}
