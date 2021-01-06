package de.deftk.openlonet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.deftk.lonet.api.implementation.Group
import de.deftk.lonet.api.implementation.feature.board.BoardNotification
import de.deftk.lonet.api.model.feature.board.BoardNotificationColor
import de.deftk.openlonet.R
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies

class NotificationAdapter(context: Context, elements: List<Pair<BoardNotification, Group>>): FilterableAdapter<Pair<BoardNotification, Group>>(context, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_notification, parent, false)
        val item = getItem(position) ?: return listItemView
        val notification = item.first

        listItemView.findViewById<TextView>(R.id.notification_title).text = notification.getTitle()
        listItemView.findViewById<TextView>(R.id.notification_author).text = notification.getCreated().member.name
        listItemView.findViewById<TextView>(R.id.notification_date).text = TextUtils.parseShortDate(notification.getModified().date)
        listItemView.findViewById<View>(R.id.notification_accent).setBackgroundResource(BoardNotificationColors.getByApiColor(notification.getColor())?.androidColor ?: BoardNotificationColors.BLUE.androidColor)
        return listItemView
    }

    override fun search(constraint: String?): List<Pair<BoardNotification, Group>> {
        if (constraint == null)
            return originalElements
        return originalElements.filter {
            it.first.getTitle().filterApplies(constraint)
                    || it.first.getText().filterApplies(constraint)
                    || it.first.getCreated().member.filterApplies(constraint)
        }
    }

    override fun sort(elements: List<Pair<BoardNotification, Group>>): List<Pair<BoardNotification, Group>> {
        return elements.sortedByDescending { it.first.getCreated().date }
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