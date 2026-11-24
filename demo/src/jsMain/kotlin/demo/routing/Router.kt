package demo.routing

import demo.pages.Page
import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState
import web.events.Event
import web.events.EventHandler
import web.events.EventType
import web.events.addEventListener
import web.window.window

/**
 * Routing as an object, not a library.
 *
 * Hash routing on purpose: the demo is a static directory inside the Pages site, so `/demo/todos`
 * would 404 on a reload unless the host knew about it. `#/todos` needs no server configuration and
 * survives a reload.
 *
 * The table holds metadata only. Entering a route builds its page and swaps it in together with
 * [path]; the page that was left is released instead of cached, so visiting a route does not
 * accumulate page state for the lifetime of the app.
 *
 * [current] is a computed and [page] is observable on purpose: replacing the instance is what
 * invalidates it. The instance is a class, not a plain object, so MobX keeps the reference as-is
 * instead of wrapping it.
 */
@OopReactState
class Router(
    private val routes: List<Route>,
) {
    init {
        require(routes.isNotEmpty()) { "Router needs at least one route." }
    }

    /** The observable bit: the current route is a path. */
    var path: String = initialPath()

    /** The page of the current route; the only one alive. */
    private var page: Page = routeOf(path).create()

    init {
        // The graph lives for the whole application, so a listener outliving it is fine here; an
        // application that can rebuild its graph would register it through OopReactCleanup.
        window.addEventListener(EventType<Event>("hashchange"), EventHandler { sync() })
    }

    val current: Page
        get() = page

    fun goPath(path: String) {
        val route = routeOf(path)
        val hash = "#/${route.path}"
        if (window.location.hash != hash) {
            window.location.hash = hash
        }

        if (route.path == this.path) return

        // Build before swapping: a page constructor that throws leaves the current route intact
        // instead of half-applied.
        val next = route.create()
        page = next
        this.path = route.path
    }

    private fun sync() {
        goPath(match(window.location.hash)?.path ?: routes.first().path)
    }

    private fun initialPath(): String = match(window.location.hash)?.path ?: routes.first().path

    private fun routeOf(path: String): Route = routes.firstOrNull { it.path == path } ?: routes.first()

    private fun match(hash: String): Route? {
        val path = hash.removePrefix("#").removePrefix("/")
        return routes.firstOrNull { it.path == path }
    }
}
