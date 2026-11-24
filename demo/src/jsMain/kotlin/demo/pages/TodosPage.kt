package demo.pages

import demo.app.Section
import demo.app.inputValue
import demo.app.Settings
import demo.i18n.Msg
import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactList
import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState
import io.github.shijianjs.ktjs.oopreact.render.OopReactRender
import io.github.shijianjs.ktjs.oopreact.render.jsx
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Checkbox
import mui.material.Stack
import mui.material.StackDirection
import mui.material.FormControl
import mui.material.InputLabel
import mui.material.OutlinedInput
import mui.material.Typography
import mui.material.styles.TypographyVariant
import mui.system.responsive
import react.ReactNode
import web.cssom.ClassName

/**
 * A list that is a plain field.
 *
 * [OopReactList] is the mutable list whose changes are observable: `items.add(...)` re-renders the
 * island that reads the list, once. Each row is its own island and subscribes to its own item, so
 * ticking one box re-runs one row.
 */
@OopReactState
class TodosPage(
    private val settings: Settings,
) : Page {
    val items: MutableList<Todo> = OopReactList()

    var draft: String = ""

    override fun content(): ReactNode {
        // A local, because inside the element DSL a bare name resolves against the element's props
        // first — `draft` on a `TextField` would be the input's own attribute, not this field.
        val draft = this.draft
        return jsx {
            Section(settings.t(Msg.todosTitle), settings.t(Msg.todosHint)) {
                Stack {
                    direction = responsive(StackDirection.row)
                    className = ClassName("app-row")
                    FormControl {
                        InputLabel { +settings.t(Msg.todoPlaceholder) }
                        OutlinedInput {
                            value = draft
                            onChange = { event -> this@TodosPage.draft = inputValue(event) }
                        }
                    }
                    Button {
                        variant = ButtonVariant.contained
                        disabled = draft.isBlank()
                        onClick = { add() }
                        +settings.t(Msg.add)
                    }
                }
                +list()
                +summary()
            }
        }
    }

    private fun add() {
        val title = draft.trim()
        if (title.isEmpty()) return

        items.add(Todo(id = (items.maxOfOrNull { it.id } ?: 0) + 1, title = title))
        draft = ""
    }

    /** Reads the list itself, so only adding or removing re-runs it. */
    @OopReactRender
    @JsName("list")
    fun list(): ReactNode {
        val current = items
        return jsx {
            Stack {
                if (current.isEmpty()) {
                    Typography {
                        className = ClassName("app-muted")
                        +"—"
                    }
                }
                for (item in current) {
                    +row(item)
                }
            }
        }
    }

    /** Reads only the done flags, so ticking a box re-runs this line and one row. */
    @OopReactRender
    @JsName("summary")
    fun summary(): ReactNode {
        val remaining = items.count { !it.done }
        return jsx {
            Typography {
                variant = TypographyVariant.body2
                className = ClassName("app-muted")
                +settings.t(Msg.remaining, "count" to remaining)
            }
        }
    }

    /**
     * One row. The key comes from [rowKey] — a method rather than a field, because Kotlin/JS renames
     * fields and a keyed island has to find the name at runtime.
     */
    @OopReactRender(key = "rowKey")
    @JsName("row")
    fun row(item: Todo): ReactNode {
        val done = item.done
        val title = item.title
        return jsx {
            Stack {
                direction = responsive(StackDirection.row)
                className = ClassName("app-row")
                Checkbox {
                    checked = done
                    onChange = { _, checked -> item.done = checked }
                }
                Typography {
                    variant = TypographyVariant.body1
                    className = ClassName(if (done) "app-spacer app-muted" else "app-spacer")
                    +title
                }
                Button {
                    variant = ButtonVariant.text
                    onClick = { items.remove(item) }
                    +settings.t(Msg.remove)
                }
            }
        }
    }

    /** The React key of [row]; produced where the element is created, not returned from the method. */
    @JsName("rowKey")
    fun rowKey(item: Todo): String = item.id.toString()
}
