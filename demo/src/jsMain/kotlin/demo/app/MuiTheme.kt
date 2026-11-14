package demo.app

import mui.material.PaletteMode
import mui.material.styles.Theme
import mui.material.styles.createTheme

/**
 * The MUI theme for one palette mode.
 *
 * `createTheme` takes a `ThemeOptions`, which in the Kotlin wrapper is a plain external interface
 * (not a `@JsPlainObject`), so the object is written as a JS literal. Assigning the literal straight
 * to the typed parameter is the shortest typed path — no cast, no builder.
 */
internal fun muiTheme(mode: PaletteMode): Theme {
    val value = mode
    return createTheme(
        js(
            """({
                palette: {mode: value},
                shape: {borderRadius: 10},
                typography: {fontFamily: 'system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", sans-serif'},
            })""",
        ),
    )
}
