package de.deftk.openww.android.feature.overview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.deftk.openww.android.R

class NotificationsOverview(val count: Int): AbstractOverviewElement(object : AbstractOverviewAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup, context: Context): View {
        val itemView = LayoutInflater.from(context).inflate(R.layout.list_item_overview_notifications, parent, false)
        itemView.findViewById<TextView>(R.id.overview_notifications_count).text = context.resources.getQuantityString(R.plurals.notification_count, count).format(count)
        return itemView
    }
})