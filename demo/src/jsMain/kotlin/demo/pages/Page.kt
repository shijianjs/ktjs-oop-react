package demo.pages

import demo.i18n.Msg
import react.ReactNode

/**
 * One page of the demo: a route, a title, and the element it renders.
 *
 * [content] is deliberately not a render method itself — a page that needs its own subscription
 * exposes one or more `@OopReactRender` methods and `content` just assembles their elements. That is
 * what keeps the islands free to be as fine-grained as the page wants.
 */
interface Page {
    val path: String

    val title: Msg

    fun content(): ReactNode
}
