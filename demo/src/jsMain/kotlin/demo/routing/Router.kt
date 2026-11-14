package demo.routing

import demo.pages.Page
import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState
import web.events.EventHandler
import web.events.EventType
import web.events.Event
import web.events.addEventListener
import web.window.window

/**
 * Routing as an object, not a library.
 *
 * Hash routing on purpose: the demo is a static directory inside the Pages site, so `/demo/todos`
 * would 404 on a reload unless the host knew about it. `#/todos` needs no server configuration and
 * survives a reload.
 *
 * The observable bit is [path]; the current page is resolved from it, so a page switch re-renders the
 * content island and nothing else.
 */
@OopReactState
class Router(
    private val pages: List<Page>,
) {
    var path: String = initialPath()

    init {
        require(pages.isNotEmpty()) { "Router needs at least one page." }

        // The graph lives for the whole application, so a listener outliving it is fine here; an
        // application that can rebuild its graph would register it through OopReactCleanup.
        window.addEventListener(EventType<Event>("hashchange"), EventHandler { sync() })
    }

    val current: Page
        get() = byPath(path)

    fun byPath(path: String): Page = pages.firstOrNull { it.path == path } ?: pages.first()

    fun go(page: Page) {
        goPath(page.path)
    }

    fun goPath(path: String) {
        this.path = byPath(path).path

        val hash = "#/$path"
        if (window.location.hash != hash) {
            window.location.hash = hash
        }
    }

    private fun sync() {
        path = match(window.location.hash)?.path ?: pages.first().path
    }

    private fun initialPath(): String = match(window.location.hash)?.path ?: pages.first().path

    private fun match(hash: String): Page? {
        val path = hash.removePrefix("#").removePrefix("/")
        return pages.firstOrNull { it.path == path }
    }
}
