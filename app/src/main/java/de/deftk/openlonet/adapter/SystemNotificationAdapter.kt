package de.deftk.openlonet.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.daimajia.swipe.SwipeLayout
import de.deftk.lonet.api.model.abstract.ManageableType
import de.deftk.lonet.api.model.feature.SystemNotification
import de.deftk.openlonet.R
import de.deftk.openlonet.utils.SwipeAdapter
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SystemNotificationAdapter(context: Context, elements: List<SystemNotification>): FilterableAdapter<SystemNotification>(context, elements.toMutableList()) {

    companion object {
        val typeTranslationMap = mapOf(
            Pair(SystemNotification.SystemNotificationType.FILE_DOWNLOAD, R.string.system_notification_type_file_download),
            Pair(SystemNotification.SystemNotificationType.FILE_UPLOAD, R.string.system_notification_type_file_upload),
            Pair(SystemNotification.SystemNotificationType.NEW_NOTIFICATION, R.string.system_notification_type_new_notification),
            Pair(SystemNotification.SystemNotificationType.NEW_TRUST, R.string.system_notification_type_new_trust),
            Pair(SystemNotification.SystemNotificationType.UNAUTHORIZED_LOGIN_LOCATION, R.string.system_notification_unauthorized_login_location),
            Pair(SystemNotification.SystemNotificationType.NEW_TASK, R.string.system_notification_type_new_task)
        )
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = (convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_system_notification, parent, false)) as SwipeLayout
        val item = getItem(position) ?: return listItemView

        listItemView.showMode = SwipeLayout.ShowMode.PullOut

        val swp = object : SwipeAdapter() {
            override fun onOpen(layout: SwipeLayout) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        item.delete()
                        withContext(Dispatchers.Main) {
                            remove(item)
                            notifyDataSetChanged()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.error).format(e.message ?: e), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        // clear all swipe listeners; there should only be one. sadly there is no official way to do this
        (listItemView::class.java.getDeclaredField("mSwipeListeners").apply { this.isAccessible = true }.get(listItemView) as ArrayList<*>).clear()

        listItemView.addSwipeListener(swp)

        val type = item.messageType
        val titleView = listItemView.findViewById<TextView>(R.id.system_notification_title)
        titleView.text = if (type != null) {
            context.getString(typeTranslationMap[type] ?: R.string.system_notification_type_unknown)
        } else {
            context.getString(R.string.system_notification_type_unknown)
        }
        if (item.read)
            titleView.setTypeface(null, Typeface.NORMAL)
        else
            titleView.setTypeface(null, Typeface.BOLD)
        listItemView.findViewById<TextView>(R.id.system_notification_author).text = if (item.group.getType() != ManageableType.UNKNOWN) item.group.getName() else item.member.getName()
        listItemView.findViewById<TextView>(R.id.system_notification_date).text = TextUtils.parseShortDate(item.date)
        return listItemView
    }

    override fun search(constraint: String?): List<SystemNotification> {
        if (constraint == null)
            return originalElements
        return originalElements.filter {
            context.getString(typeTranslationMap[it.messageType] ?: R.string.system_notification_type_unknown).filterApplies(constraint)
                    || it.message.filterApplies(constraint)
                    || it.member.filterApplies(constraint)
                    || it.group.filterApplies(constraint)
        }
    }

    override fun sort(elements: List<SystemNotification>): List<SystemNotification> {
        return elements.sortedByDescending { it.date }
    }
}