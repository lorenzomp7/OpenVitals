package tech.mmarca.openvitals.core.presentation

/**
 * Moves [item] to [target]'s place among the [visible] items of [full], the visible items between
 * them shifting by one. The items that are not visible keep their slots, so a move and its reverse
 * leave [full] exactly as it was: an edit undone by hand is no edit.
 */
fun <T> moveWithinVisible(full: List<T>, visible: Set<T>, item: T, target: T): List<T> {
    val order = full.filter { it in visible }.toMutableList()
    val from = order.indexOf(item)
    val to = order.indexOf(target)
    if (from == -1 || to == -1 || from == to) return full
    order.removeAt(from)
    order.add(to, item)
    val next = order.iterator()
    return full.map { if (it in visible) next.next() else it }
}

/** [full] with its [newVisibleOrder] items written, in order, into the slots its visible items held. */
fun <T> replaceVisibleOrder(full: List<T>, newVisibleOrder: List<T>): List<T> {
    val visible = newVisibleOrder.toSet()
    if (full.count { it in visible } != newVisibleOrder.size) return full
    val next = newVisibleOrder.iterator()
    return full.map { if (it in visible) next.next() else it }
}
