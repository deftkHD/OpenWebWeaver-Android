package de.deftk.openlonet.filter

import androidx.annotation.StringRes

abstract class Filter<T>(var order: Order<T> = Order.None()) {

    val criterias = mutableListOf<Criteria<T, *>>()

    open fun apply(items: List<T>): List<T> {
        return order.sort(items.filter { item -> criterias.all { it.matches(item) } })
    }

    fun <K> addCriteria(@StringRes nameRes: Int, value: K?, matcher: (T, K?) -> Boolean): Criteria<T, K?> {
        val criteria = object : Criteria<T, K?>(nameRes, value) {
            override fun matches(element: T): Boolean {
                return matcher(element, value)
            }
        }
        criterias.add(criteria)
        return criteria
    }

    abstract inner class Criteria<T, K>(@StringRes val nameRes: Int, var value: K?) {
        abstract fun matches(element: T): Boolean
    }

}

