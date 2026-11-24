package demo.app

import demo.i18n.Msg
import demo.pages.CounterPage
import demo.pages.FailurePage
import demo.pages.OrdersPage
import demo.pages.TicTacToePage
import demo.pages.TodosPage
import demo.routing.Route
import demo.routing.Router
import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState
import io.github.shijianjs.ktjs.oopreact.render.OopReactRender
import io.github.shijianjs.ktjs.oopreact.render.jsx
import mui.material.Alert
import mui.material.AppBar
import mui.material.AppBarPosition
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Container
import mui.material.CssBaseline
import mui.material.Snackbar
import mui.material.Stack
import mui.material.StackDirection
import mui.material.Toolbar
import mui.material.Typography
import mui.material.styles.ThemeProvider
import mui.material.styles.TypographyVariant
import mui.system.responsive
import react.Key
import react.ReactNode
import web.cssom.ClassName

/**
 * The root of the graph: settings, the router, one shared feedback channel, and the route table.
 *
 * The table is metadata plus a factory per entry; the router builds a page only when its route is
 * entered. That is what keeps a long menu from instantiating every page — and every page's state —
 * up front.
 *
 * It also owns the whole page — the theme provider, the layout, and each part of the layout as its
 * own island — so that switching a page does not re-render the header and a snackbar does not
 * re-render the menu. There is no separate "React layer": the entry file renders [root] and React
 * never sees anything else.
 *
 * Layout is MUI's own props (`Container`, `Stack`) plus a handful of classes from `demo.css`; there
 * is no `sx` object anywhere, which is also why the render methods stay readable.
 */
@OopReactState
class App {
    val settings: Settings = Settings()

    val feedback: Feedback = Feedback()

    val routes: List<Route> = listOf(
        Route("counter", Msg.navCounter) { CounterPage(settings) },
        Route("todos", Msg.navTodos) { TodosPage(settings) },
        Route("tictactoe", Msg.navTictactoe) { TicTacToePage(settings) },
        Route("orders", Msg.navOrders) { OrdersPage(feedback, settings) },
        Route("failure", Msg.navFailure) { FailurePage(settings) },
    )

    val router: Router = Router(routes)

    /** Theme and locale wrap the tree, which is exactly what a render method does. */
    @OopReactRender
    @JsName("root")
    fun root(): ReactNode {
        val theme = settings.theme
        return jsx {
            ThemeProvider {
                this.theme = muiTheme(theme)
                CssBaseline { }
                +shell()
            }
        }
    }

    @OopReactRender
    @JsName("shell")
    fun shell(): ReactNode =
        jsx {
            +header()
            Container {
                maxWidth = "md"
                Stack {
                    className = ClassName("app-page app-stack")
                    +nav()
                    +content()
                }
            }
            +feedbackBar()
        }

    /** Reads the theme and the locale, so a switch re-renders the bar and nothing below it. */
    @OopReactRender
    @JsName("header")
    fun header(): ReactNode {
        val settings = this.settings
        return jsx {
            AppBar {
                position = AppBarPosition.static
                Toolbar {
                    Typography {
                        variant = TypographyVariant.h6
                        className = ClassName("app-spacer")
                        +settings.t(Msg.appTitle)
                    }
                    Typography {
                        variant = TypographyVariant.caption
                        className = ClassName("app-muted app-spacer")
                        +settings.t(Msg.tagline)
                    }
                    Button {
                        onClick = { settings.toggleTheme() }
                        +settings.t(Msg.toggleTheme)
                    }
                    Button {
                        onClick = { settings.toggleLocale() }
                        +settings.t(Msg.toggleLocale)
                    }
                }
            }
        }
    }

    /** Reads only the route, so a page switch re-renders the nav and nothing else. */
    @OopReactRender
    @JsName("nav")
    fun nav(): ReactNode {
        val router = this.router
        val settings = this.settings
        val routes = this.routes
        val active = router.path
        return jsx {
            Stack {
                direction = responsive(StackDirection.row)
                className = ClassName("app-nav")
                for (route in routes) {
                    Button {
                        key = Key(route.path)
                        variant = if (route.path == active) ButtonVariant.contained else ButtonVariant.text
                        onClick = { router.goPath(route.path) }
                        +settings.t(route.title)
                    }
                }
            }
        }
    }

    /** Reads only `router.current`, so a page switch re-renders this island alone. */
    @OopReactRender
    @JsName("content")
    fun content(): ReactNode {
        val page = router.current
        return jsx {
            +page.content()
        }
    }

    @OopReactRender
    @JsName("feedbackBar")
    fun feedbackBar(): ReactNode {
        val feedback = this.feedback
        val text = feedback.message
        return jsx {
            Snackbar {
                open = feedback.open
                autoHideDuration = 2500
                onClose = { _, _ -> feedback.dismiss() }
                Alert {
                    onClose = { feedback.dismiss() }
                    +text
                }
            }
        }
    }
}
