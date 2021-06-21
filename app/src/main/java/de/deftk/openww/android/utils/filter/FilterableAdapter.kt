package de.deftk.openww.android.utils.filter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import java.util.*

@Deprecated("use a different approach with view models in mind")
abstract class FilterableAdapter<T>(protected val context: Context, elements: List<T>) :
    BaseAdapter(), Filterable {

    private val filter = AdapterFilter()

    protected val originalElements = elements.toMutableList()
    private val _elements by lazy { sort(originalElements).toMutableList() }

    abstract override fun getView(position: Int, convertView: View?, parent: ViewGroup): View

    open fun search(constraint: String?): List<T> {
        return originalElements
    }

    open fun sort(elements: List<T>): List<T> {
        return elements
    }

    override fun getCount(): Int {
        return _elements.size
    }

    override fun getItem(position: Int): T? {
        return _elements[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun remove(obj: T) {
        _elements.remove(obj)
        originalElements.remove(obj)
    }

    fun getPosition(obj: T) = _elements.indexOf(obj)

    fun insert(obj: T, index: Int) {
        _elements.add(index, obj)
        originalElements.add(index, obj)
    }

    fun getElements(): List<T> = _elements

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
                _elements.clear()
                @Suppress("UNCHECKED_CAST")
                _elements.addAll(result as List<T>)
                notifyDataSetChanged()
            }
        }
    }



}