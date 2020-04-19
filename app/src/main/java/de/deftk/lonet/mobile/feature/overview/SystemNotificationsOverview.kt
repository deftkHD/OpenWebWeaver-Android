package de.deftk.lonet.mobile.feature.overview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.fragments.SystemNotificationsFragment

class SystemNotificationsOverview(val unreadSystemNotifications: Int): AbstractOverviewElement(object : AbstractOverviewAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup, context: Context): View {
        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_overview_system_notifications, parent, false)
        itemView.findViewById<TextView>(R.id.overview_system_notifications_unread).text = String.format(context.getString(R.string.unread), unreadSystemNotifications)
        return itemView
    }
})