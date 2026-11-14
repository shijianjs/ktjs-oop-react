package demo.app

import demo.i18n.Msg
import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState
import mui.material.PaletteMode
import web.cssom.matchMedia
import web.cssom.MediaQuery
import web.dom.document

enum class Locale { zh, en }

/**
 * Theme and language are state like any other, so they live on an object instead of in a context or
 * a hook. Everything that calls [t] subscribes to `locale`; everything that reads [theme] subscribes
 * to it — and nothing else re-renders when either changes.
 *
 * The `<html>` attributes are written by the setters, not by a reaction: the effect happens exactly
 * where the state changes, which is both shorter and the conventional object-oriented shape.
 */
@OopReactState
class Settings {
    var theme: PaletteMode = preferredTheme()

    var locale: Locale = Locale.zh

    init {
        applyToDocument()
    }

    fun toggleTheme() {
        setTheme(if (theme == PaletteMode.dark) PaletteMode.light else PaletteMode.dark)
    }

    fun setTheme(value: PaletteMode) {
        theme = value
        applyToDocument()
    }

    fun toggleLocale() {
        locale = if (locale == Locale.zh) Locale.en else Locale.zh
        applyToDocument()
    }

    /** Reads [locale], so a render method that calls this re-renders on a switch. */
    fun t(message: Msg): String = if (locale == Locale.zh) message.zh else message.en

    fun t(message: Msg, vararg values: Pair<String, Any>): String {
        var text = t(message)
        for ((name, value) in values) {
            text = text.replace("{$name}", value.toString())
        }
        return text
    }

    private fun applyToDocument() {
        document.documentElement.setAttribute("data-theme", if (theme == PaletteMode.dark) "dark" else "light")
        document.documentElement.lang = if (locale == Locale.zh) "zh-Hans" else "en"
    }
}

private fun preferredTheme(): PaletteMode =
    if (matchMedia(MediaQuery("(prefers-color-scheme: dark)")).matches) PaletteMode.dark else PaletteMode.light
