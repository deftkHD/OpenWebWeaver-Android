package de.deftk.openlonet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import de.deftk.lonet.api.model.feature.board.BoardNotification
import de.deftk.lonet.api.model.feature.board.BoardNotificationColor
import de.deftk.openlonet.R
import java.text.DateFormat

class NotificationAdapter(context: Context, elements: List<BoardNotification>): ArrayAdapter<BoardNotification>(context, 0, elements.toMutableList()) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_notification, parent, false)
        val item = getItem(position) ?: return listItemView

        listItemView.findViewById<TextView>(R.id.notification_title).text = item.title
        listItemView.findViewById<TextView>(R.id.notification_author).text = item.creationMember.getName()
        listItemView.findViewById<TextView>(R.id.notification_date).text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(item.creationDate)
        listItemView.findViewById<View>(R.id.notification_accent).setBackgroundResource(BoardNotificationColors.getByApiColor(item.color)?.androidColor ?: BoardNotificationColors.BLUE.androidColor)
        return listItemView
    }

    enum class BoardNotificationColors(val apiColor: BoardNotificationColor, val androidColor: Int, val text: Int) {
        BLUE(BoardNotificationColor.BLUE, android.R.color.holo_blue_light, R.string.blue),
        GREEN(BoardNotificationColor.GREEN, android.R.color.holo_green_light, R.string.green),
        RED(BoardNotificationColor.RED, android.R.color.holo_red_light, R.string.red),
        YELLOW(BoardNotificationColor.YELLOW, android.R.color.holo_orange_light, R.string.yellow),
        WHITE(BoardNotificationColor.WHITE, android.R.color.white, R.string.white);

        companion object {
            fun getByApiColor(color: BoardNotificationColor?): BoardNotificationColors? {
                if (color == null) return null
                return values().firstOrNull { it.apiColor == color }
            }
        }
    }

}