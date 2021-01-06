package de.deftk.openlonet.utils.filter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import java.util.*

abstract class FilterableAdapter<T>(protected val context: Context, elements: List<T>) :
    BaseAdapter(), Filterable {

    private val filter = AdapterFilter()

    protected val originalElements = elements.toMutableList()
    private val elements by lazy { sort(originalElements).toMutableList() }

    abstract override fun getView(position: Int, convertView: View?, parent: ViewGroup): View

    open fun search(constraint: String?): List<T> {
        return originalElements
    }

    open fun sort(elements: List<T>): List<T> {
        return elements
    }

    override fun getCount(): Int {
        return elements.size
    }

    override fun getItem(position: Int): T? {
        return elements[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun remove(obj: T) {
        elements.remove(obj)
        originalElements.remove(obj)
    }

    fun getPosition(obj: T) = elements.indexOf(obj)

    fun insert(obj: T, index: Int) {
        elements.add(index, obj)
        originalElements.add(index, obj)
    }

    override fun getFilter(): Filter {
        return filter
    }

    private inner class AdapterFilter : Filter() {

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val result = FilterResults()
            val filtered = sort(search(constraint?.toString()?.toLowerCase(Locale.getDefault())))
            result.values = filtered
            result.count = filtered.size
            return result
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            val result = results.values
            if (result is List<*>) {
                elements.clear()
                @Suppress("UNCHECKED_CAST")
                elements.addAll(result as List<T>)
                notifyDataSetChanged()
            }
        }
    }

}