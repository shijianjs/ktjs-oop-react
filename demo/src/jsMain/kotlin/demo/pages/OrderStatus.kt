package demo.pages

import demo.i18n.Msg

/** The three states a row is allowed to be in; the label is the translated one. */
enum class OrderStatus(
    val label: Msg,
) {
    New(Msg.statusNew),
    Paid(Msg.statusPaid),
    Shipped(Msg.statusShipped),
}
