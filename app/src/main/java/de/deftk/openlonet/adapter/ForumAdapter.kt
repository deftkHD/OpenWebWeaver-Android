package de.deftk.openlonet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.deftk.lonet.api.implementation.Group
import de.deftk.openlonet.R
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies

class ForumAdapter(context: Context, elements: List<Group>): FilterableAdapter<Group>(context, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_forum, parent, false)
        val item = getItem(position) ?: return listItemView
        listItemView.findViewById<TextView>(R.id.forum_name).text = item.name
        return listItemView
    }

    override fun search(constraint: String?): List<Group> {
        if (constraint == null)
            return originalElements
        return originalElements.filter { it.filterApplies(constraint) }
    }

    override fun sort(elements: List<Group>): List<Group> {
        return elements.sortedBy { it.name }
    }
}