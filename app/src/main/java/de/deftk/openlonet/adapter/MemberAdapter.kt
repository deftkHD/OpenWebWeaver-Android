package de.deftk.openlonet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import de.deftk.lonet.api.implementation.Group
import de.deftk.lonet.api.model.IScope
import de.deftk.lonet.api.model.RemoteScope
import de.deftk.openlonet.R
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies

class MemberAdapter(context: Context, elements: List<IScope>) :
    FilterableAdapter<IScope>(context, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)!!
        return if (item !is Group) {
            val listItemView = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.list_item_member, parent, false)
            listItemView.findViewById<TextView>(R.id.member_name).text = item.name
            if (item is RemoteScope) {
                listItemView.findViewById<TextView>(R.id.member_online_status)
                    .setText(if (item.isOnline) R.string.online else R.string.offline)
                listItemView.findViewById<ImageView>(R.id.member_image)
                    .setImageResource(if (item.isOnline) R.drawable.ic_person_orange_24 else R.drawable.ic_person_24)
            }
            listItemView
        } else {
            val listItemView = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.list_item_member_group, parent, false)
            listItemView.findViewById<TextView>(R.id.member_group_name).text = item.name
            listItemView
        }
    }

    override fun search(constraint: String?): List<IScope> {
        if (constraint == null)
            return originalElements
        return originalElements.filter { it.filterApplies(constraint) }
    }

    override fun sort(elements: List<IScope>): List<IScope> {
        return elements.sortedWith(
            compareBy(
                {
                    val index = it.name.indexOf('.')
                    if (index == -1)
                        Int.MAX_VALUE
                    else index
                },
                { it.name })
        )
    }

}