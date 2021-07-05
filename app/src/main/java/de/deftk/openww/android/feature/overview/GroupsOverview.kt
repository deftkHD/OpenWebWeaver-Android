package de.deftk.openww.android.feature.overview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.deftk.openww.android.R

class GroupsOverview(val groupCount: Int) : AbstractOverviewElement(object : AbstractOverviewAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup, context: Context): View {
        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_overview_groups, parent, false)
        itemView.findViewById<TextView>(R.id.overview_groups_count).text = context.getString(R.string.group_count).format(groupCount)
        return itemView
    }

})