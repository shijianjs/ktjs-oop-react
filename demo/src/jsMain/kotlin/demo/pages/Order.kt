package demo.pages

import io.github.shijianjs.ktjs.oopreact.reactivity.OopReactState

/** A row of the orders table. Observable: editing its fields re-renders the cell that reads them. */
@OopReactState
class Order(
    val id: Int,
    var customer: String,
    var amount: Double,
    var status: OrderStatus,
)
