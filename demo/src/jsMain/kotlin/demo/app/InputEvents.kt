package demo.app

import react.dom.events.ChangeEvent
import web.html.HTMLElement

/**
 * The text of the input a change event came from.
 *
 * The wrapper types a MUI change handler as `ChangeEventHandler<HTMLElement, *>`, so `event.target`
 * is an unspecified `Element` and `value` is not on it. React hands over the input element itself,
 * which is why reading `target.value` is right; the JS expression says that in one place instead of
 * a cast at every call site.
 */
internal fun inputValue(event: ChangeEvent<HTMLElement, *>): String = js("event.target.value")
