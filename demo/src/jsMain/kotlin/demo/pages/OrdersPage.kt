package demo.pages

import demo.app.Feedback
import demo.app.Section
import demo.app.inputValue
import demo.app.Settings
import demo.i18n.Msg
import io.github.shijianjs.ktjs.oopreact.async.OopReactAsyncView
import io.github.shijianjs.ktjs.oopreact.async.OopReactResource
import io.github.shijianjs.ktjs.oopreact.async.OopReactTaskResult
import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState
import io.github.shijianjs.ktjs.oopreact.render.OopReactRender
import io.github.shijianjs.ktjs.oopreact.render.jsx
import io.github.shijianjs.ktjs.oopreact.selection.OopReactSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Checkbox
import mui.material.Dialog
import mui.material.DialogActions
import mui.material.DialogContent
import mui.material.DialogTitle
import mui.material.Stack
import mui.material.StackDirection
import mui.material.Table
import mui.material.TableBody
import mui.material.TableCell
import mui.material.TableHead
import mui.material.TableRow
import mui.material.FormControl
import mui.material.InputLabel
import mui.material.OutlinedInput
import mui.material.Typography
import mui.material.styles.TypographyVariant
import mui.system.responsive
import react.Key
import react.ReactNode
import web.cssom.ClassName

/**
 * Delayed data, a controlled table and a dialog form.
 *
 * Three objects do the work: [OopReactResource] holds the rows and their loading/error state,
 * [OopReactSelection] holds the selected ids, and the page holds the draft being edited. Each is a
 * plain field, so each re-renders only the island that reads it.
 *
 * A click starts an async job; the job writes observable fields like any other method, and MobX
 * batches each write into one notification. The `saving` flag is not `saving = true … await` inside
 * one action — it cannot be, an `await` ends the action — it is simply two states of the same field.
 */
@OopReactState
class OrdersPage(
    private val feedback: Feedback,
    private val settings: Settings,
) : Page {
    override val path: String = "orders"

    override val title: Msg = Msg.navOrders

    val rows: OopReactResource<List<Order>> = OopReactResource()

    val selection: OopReactSelection<Int> = OopReactSelection()

    var draft: Order? = null

    var draftCustomer: String = ""

    var draftAmount: String = ""

    var draftStatus: OrderStatus = OrderStatus.New

    var validationError: Msg? = null

    var saving: Boolean = false

    init {
        refresh()
    }

    override fun content(): ReactNode = jsx {
        Section(settings.t(Msg.ordersTitle), settings.t(Msg.ordersHint)) {
            +toolbar()
            +table()
            +editor()
        }
    }

    fun refresh() {
        scope.launch {
            rows.load { fetchOrders() }
        }
    }

    fun openNew() {
        draft = Order(id = nextId(), customer = "", amount = 0.0, status = OrderStatus.New)
        draftCustomer = ""
        draftAmount = ""
        draftStatus = OrderStatus.New
        validationError = null
    }

    fun openEdit(order: Order) {
        draft = order
        draftCustomer = order.customer
        draftAmount = order.amount.toString()
        draftStatus = order.status
        validationError = null
    }

    fun closeEditor() {
        draft = null
        validationError = null
    }

    fun save() {
        val order = draft ?: return
        val customer = draftCustomer.trim()
        val amount = draftAmount.trim().toDoubleOrNull()

        validationError =
            when {
                customer.isEmpty() -> Msg.customerRequired
                amount == null || amount <= 0.0 -> Msg.amountInvalid
                else -> null
            }

        if (validationError != null) return

        saving = true
        scope.launch {
            val result = rows.loading.run { delay(400) }
            saving = false
            if (result is OopReactTaskResult.Failure) {
                feedback.show(result.error.toString())
                return@launch
            }

            order.customer = customer
            order.amount = amount ?: 0.0
            order.status = draftStatus

            val current = rows.data.orEmpty()
            if (current.none { it.id == order.id }) {
                rows.data = current + order
            }

            draft = null
            feedback.show(settings.t(Msg.saved))
        }
    }

    fun removeSelected() {
        val selected = selection.ids
        scope.launch {
            delay(200)
            rows.data = rows.data.orEmpty().filterNot { it.id in selected }
            selection.clear()
            feedback.show(settings.t(Msg.deleted))
        }
    }

    @OopReactRender
    @JsName("toolbar")
    fun toolbar(): ReactNode {
        val settings = this.settings
        val loading = rows.loading.loading
        val selected = selection.ids
        return jsx {
            Stack {
                direction = responsive(StackDirection.row)
                className = ClassName("app-row")
                Button {
                    variant = ButtonVariant.contained
                    onClick = { openNew() }
                    +settings.t(Msg.newOrder)
                }
                Button {
                    variant = ButtonVariant.outlined
                    disabled = loading
                    onClick = { refresh() }
                    +settings.t(Msg.refresh)
                }
                Button {
                    variant = ButtonVariant.text
                    disabled = selected.isEmpty()
                    onClick = { removeSelected() }
                    +settings.t(Msg.clearSelection)
                }
                Typography {
                    variant = TypographyVariant.body2
                    className = ClassName("app-muted")
                    +settings.t(Msg.selectedCount, "count" to selected.size)
                }
            }
        }
    }

    /** The three screens of delayed data in one call: skeleton, table, failure. */
    @OopReactRender
    @JsName("table")
    fun table(): ReactNode? =
        OopReactAsyncView<List<Order>>(
            rows,
            skeleton = {
                jsx {
                    Typography {
                        className = ClassName("app-muted")
                        +settings.t(Msg.loadingMessage)
                    }
                }
            },
            ready = { tableContent() },
            failed = { error ->
                jsx {
                    Typography {
                        className = ClassName("app-error")
                        +settings.t(Msg.loadFailed, "message" to (error.message ?: "?"))
                    }
                }
            },
        )

    @OopReactRender
    @JsName("tableContent")
    fun tableContent(): ReactNode {
        val settings = this.settings
        val orders = rows.data.orEmpty()
        val selected = selection.ids
        return jsx {
            Table {
                TableHead {
                    TableRow {
                        TableCell { }
                        TableCell { +settings.t(Msg.customer) }
                        TableCell { +settings.t(Msg.amount) }
                        TableCell { +settings.t(Msg.status) }
                        TableCell { +settings.t(Msg.actions) }
                    }
                }
                TableBody {
                    for (order in orders) {
                        TableRow {
                            key = Key(order.id.toString())
                            TableCell {
                                Checkbox {
                                    checked = selected.contains(order.id)
                                    onChange = { _, checked -> if (checked) selection.add(order.id) else selection.remove(order.id) }
                                }
                            }
                            TableCell { +order.customer }
                            TableCell { +order.amount.toString() }
                            TableCell { +settings.t(order.status.label) }
                            TableCell {
                                Button {
                                    variant = ButtonVariant.text
                                    onClick = { openEdit(order) }
                                    +settings.t(Msg.edit)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OopReactRender
    @JsName("editor")
    fun editor(): ReactNode? {
        val order = draft ?: return null
        val settings = this.settings
        val validation = validationError
        val saving = this.saving
        val customer = draftCustomer
        val amount = draftAmount

        return jsx {
            Dialog {
                open = true
                onClose = { _, _ -> closeEditor() }
                DialogTitle {
                    +settings.t(if (order.customer.isEmpty()) Msg.newOrder else Msg.editOrder)
                }
                DialogContent {
                    Stack {
                        spacing = responsive(2)
                        className = ClassName("app-dialog-form")
                        FormControl {
                            error = validation == Msg.customerRequired
                            InputLabel { +settings.t(Msg.customer) }
                            OutlinedInput {
                                value = customer
                                onChange = { event -> this@OrdersPage.draftCustomer = inputValue(event) }
                            }
                        }
                        FormControl {
                            error = validation == Msg.amountInvalid
                            InputLabel { +settings.t(Msg.amount) }
                            OutlinedInput {
                                value = amount
                                onChange = { event -> this@OrdersPage.draftAmount = inputValue(event) }
                            }
                        }
                        Stack {
                            direction = responsive(StackDirection.row)
                            className = ClassName("app-row")
                            for (status in OrderStatus.entries) {
                                Button {
                                    variant =
                                        if (status == draftStatus) ButtonVariant.contained else ButtonVariant.outlined
                                    onClick = { draftStatus = status }
                                    +settings.t(status.label)
                                }
                            }
                        }
                        if (validation != null) {
                            Typography {
                                variant = TypographyVariant.body2
                                className = ClassName("app-error")
                                +settings.t(validation)
                            }
                        }
                    }
                }
                DialogActions {
                    Button {
                        onClick = { closeEditor() }
                        +settings.t(Msg.cancel)
                    }
                    Button {
                        variant = ButtonVariant.contained
                        disabled = saving
                        onClick = { save() }
                        +settings.t(Msg.save)
                    }
                }
            }
        }
    }

    private fun nextId(): Int = (rows.data.orEmpty().maxOfOrNull { it.id } ?: 0) + 1

    private suspend fun fetchOrders(): List<Order> {
        delay(600)
        return DEMO_ORDERS
    }
}

/** A row of the table. Observable: editing its fields re-renders the cell that reads them. */
@OopReactState
class Order(
    val id: Int,
    var customer: String,
    var amount: Double,
    var status: OrderStatus,
)

enum class OrderStatus(
    val label: Msg,
) {
    New(Msg.statusNew),
    Paid(Msg.statusPaid),
    Shipped(Msg.statusShipped),
}

private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

private val DEMO_ORDERS: List<Order> =
    listOf(
        Order(1, "Acme 纸业", 1280.0, OrderStatus.Paid),
        Order(2, "北辰物流", 540.5, OrderStatus.New),
        Order(3, "青山书店", 96.0, OrderStatus.Shipped),
    )
