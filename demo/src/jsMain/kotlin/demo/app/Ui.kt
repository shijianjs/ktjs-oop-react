package demo.app

import mui.material.Card
import mui.material.CardContent
import mui.material.Stack
import mui.material.Typography
import mui.material.styles.TypographyVariant
import mui.system.responsive
import react.ChildrenBuilder
import web.cssom.ClassName

/**
 * One card per topic: a title, a one-line explanation of what it demonstrates, and the content.
 *
 * The demo's only shared layout piece. Everything else is the page's own structure — a shared
 * "component library" would hide the point being demonstrated.
 */
internal fun ChildrenBuilder.Section(
    title: String,
    hint: String,
    content: ChildrenBuilder.() -> Unit,
) {
    Card {
        CardContent {
            Stack {
                spacing = responsive(1)
                Typography {
                    variant = TypographyVariant.h6
                    +title
                }
                Typography {
                    variant = TypographyVariant.body2
                    className = ClassName("app-muted")
                    +hint
                }
                content()
            }
        }
    }
}
