package demo.routing

import demo.i18n.Msg
import demo.pages.Page

/**
 * One entry of the routing table: what the menu needs, plus how to build the page.
 *
 * The instance is deliberately not held here. A route table is metadata — paths and menu labels,
 * resolved before anything is mounted — and the page behind an entry is created on entry. An
 * application with hundreds of routes therefore pays for the one on screen, not for all of them.
 */
class Route(
    val path: String,
    val title: Msg,
    val create: () -> Page,
)
