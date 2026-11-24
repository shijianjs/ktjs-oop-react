package demo.pages

import react.ReactNode

/**
 * A page once a route has created it: the element it renders.
 *
 * The path and the menu label are route metadata (`demo.routing.Route`), not page fields — that is
 * what lets the menu be drawn without building a single page.
 *
 * [content] is deliberately not a render method itself — a page that needs its own subscription
 * exposes one or more `@OopReactRender` methods and `content` just assembles their elements. That is
 * what keeps the islands free to be as fine-grained as the page wants.
 */
interface Page {
    fun content(): ReactNode
}
