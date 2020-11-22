package de.deftk.openlonet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import de.deftk.lonet.api.model.Group
import de.deftk.lonet.api.model.RemoteManageable
import de.deftk.lonet.api.model.abstract.IManageable
import de.deftk.openlonet.R
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies

class MemberAdapter(context: Context, elements: List<IManageable>) :
    FilterableAdapter<IManageable>(context, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)!!
        return if (item !is Group) {
            val listItemView = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.list_item_member, parent, false)
            listItemView.findViewById<TextView>(R.id.member_name).text = item.getName()
            if (item is RemoteManageable) {
                listItemView.findViewById<TextView>(R.id.member_online_status)
                    .setText(if (item.isOnline) R.string.online else R.string.offline)
                listItemView.findViewById<ImageView>(R.id.member_image)
                    .setImageResource(if (item.isOnline) R.drawable.ic_person_orange_24 else R.drawable.ic_person_24)
            }
            listItemView
        } else {
            val listItemView = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.list_item_member_group, parent, false)
            listItemView.findViewById<TextView>(R.id.member_group_name).text = item.getName()
            listItemView
        }
    }

    override fun search(constraint: String?): List<IManageable> {
        if (constraint == null)
            return originalElements
        return originalElements.filter { it.filterApplies(constraint) }
    }

    override fun sort(elements: List<IManageable>): List<IManageable> {
        return elements.sortedWith(
            compareBy(
                {
                    val index = it.getName().indexOf('.')
                    if (index == -1)
                        Int.MAX_VALUE
                    else index
                },
                { it.getName() })
        )
    }

}