package de.deftk.lonet.mobile.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import de.deftk.lonet.api.model.Group
import de.deftk.lonet.api.model.RemoteManageable
import de.deftk.lonet.api.model.abstract.IManageable
import de.deftk.lonet.mobile.R

class MemberAdapter(context: Context, elements: List<IManageable>): ArrayAdapter<IManageable>(context, 0, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)!!
        return if (item !is Group) {
            val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_member, parent, false)
            listItemView.findViewById<TextView>(R.id.member_name).text = item.getName()
            if (item is RemoteManageable) {
                listItemView.findViewById<TextView>(R.id.member_online_status).setText(if (item.isOnline) R.string.online else R.string.offline)
                listItemView.findViewById<ImageView>(R.id.member_image).setImageResource(if (item.isOnline) R.drawable.ic_member_online else R.drawable.ic_member)
            }
            listItemView
        } else {
            val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_member_group, parent, false)
            listItemView.findViewById<TextView>(R.id.member_group_name).text = item.getName()
            listItemView
        }
    }

}