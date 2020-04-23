package de.deftk.lonet.mobile.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import de.deftk.lonet.api.model.feature.Notification
import de.deftk.lonet.mobile.R
import java.text.DateFormat

class NotificationAdapter(context: Context, elements: List<Notification>): ArrayAdapter<Notification>(context, 0, elements) {

    companion object {
        val notificationColorMap = mapOf(
            Pair(Notification.NotificationColor.BLUE, android.R.color.holo_blue_light),
            Pair(Notification.NotificationColor.GREEN, android.R.color.holo_green_light),
            Pair(Notification.NotificationColor.RED, android.R.color.holo_red_light),
            Pair(Notification.NotificationColor.YELLOW, android.R.color.holo_orange_light),
            Pair(Notification.NotificationColor.WHITE, android.R.color.white)
        )
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_notification, parent, false)
        val item = getItem(position) ?: return listItemView

        listItemView.findViewById<TextView>(R.id.notification_title).text = item.title
        listItemView.findViewById<TextView>(R.id.notification_author).text = item.creationMember.getName()
        listItemView.findViewById<TextView>(R.id.notification_date).text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(item.creationDate)
        listItemView.findViewById<View>(R.id.notification_accent).setBackgroundResource(notificationColorMap.getValue(item.color))
        return listItemView
    }

}