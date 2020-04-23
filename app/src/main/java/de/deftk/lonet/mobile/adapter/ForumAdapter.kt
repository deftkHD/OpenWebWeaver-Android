package de.deftk.lonet.mobile.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import de.deftk.lonet.api.model.Group
import de.deftk.lonet.mobile.R

class ForumAdapter(context: Context, elements: List<Group>): ArrayAdapter<Group>(context, 0, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_forum, parent, false)
        val item = getItem(position) ?: return listItemView
        listItemView.findViewById<TextView>(R.id.forum_name).text = item.getName()
        return listItemView
    }

}