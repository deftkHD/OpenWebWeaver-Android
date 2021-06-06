package de.deftk.openlonet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.deftk.lonet.api.model.IGroup
import de.deftk.lonet.api.model.feature.board.IBoardNotification
import de.deftk.openlonet.R
import de.deftk.openlonet.feature.board.BoardNotificationColors
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies

@Deprecated("use recycler view instead")
class NotificationAdapter(context: Context, elements: List<Pair<IBoardNotification, IGroup>>): FilterableAdapter<Pair<IBoardNotification, IGroup>>(context, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_notification, parent, false)
        val item = getItem(position) ?: return listItemView
        val notification = item.first

        listItemView.findViewById<TextView>(R.id.notification_title).text = notification.getTitle()
        listItemView.findViewById<TextView>(R.id.notification_author).text = notification.created.member.name
        listItemView.findViewById<TextView>(R.id.notification_date).text = TextUtils.parseShortDate(notification.getModified().date)
        listItemView.findViewById<View>(R.id.notification_accent).setBackgroundResource(BoardNotificationColors.getByApiColorOrDefault(notification.getColor()).androidColor)
        return listItemView
    }

    override fun search(constraint: String?): List<Pair<IBoardNotification, IGroup>> {
        if (constraint == null)
            return originalElements
        return originalElements.filter {
            it.first.getTitle().filterApplies(constraint)
                    || it.first.getText().filterApplies(constraint)
                    || it.first.created.member.filterApplies(constraint)
        }
    }

    override fun sort(elements: List<Pair<IBoardNotification, IGroup>>): List<Pair<IBoardNotification, IGroup>> {
        return elements.sortedByDescending { it.first.created.date }
    }

}