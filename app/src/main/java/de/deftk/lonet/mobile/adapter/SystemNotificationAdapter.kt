package de.deftk.lonet.mobile.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.daimajia.swipe.SwipeLayout
import de.deftk.lonet.api.model.abstract.ManageableType
import de.deftk.lonet.api.model.feature.SystemNotification
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.utils.SwipeAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat

class SystemNotificationAdapter(context: Context, elements: List<SystemNotification>): ArrayAdapter<SystemNotification>(context, 0, elements.toMutableList()) {

    companion object {
        val typeTranslationMap = mapOf(
            Pair(SystemNotification.SystemNotificationType.FILE_DOWNLOAD, R.string.system_notification_type_file_download),
            Pair(SystemNotification.SystemNotificationType.FILE_UPLOAD, R.string.system_notification_type_file_upload),
            Pair(SystemNotification.SystemNotificationType.NEW_NOTIFICATION, R.string.system_notification_type_new_notification),
            Pair(SystemNotification.SystemNotificationType.NEW_TRUST, R.string.system_notification_type_new_trust),
            Pair(SystemNotification.SystemNotificationType.NEW_TASK, R.string.system_notification_type_new_task),
            Pair(SystemNotification.SystemNotificationType.UNKNOWN, R.string.system_notification_type_unknown)
        )
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = (convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_system_notification, parent, false)) as SwipeLayout
        val item = getItem(position) ?: return listItemView

        listItemView.showMode = SwipeLayout.ShowMode.PullOut
        listItemView.addSwipeListener(object : SwipeAdapter() {
            override fun onOpen(layout: SwipeLayout) {
                CoroutineScope(Dispatchers.IO).launch {
                    item.delete()
                    withContext(Dispatchers.Main) {
                        remove(item)
                        notifyDataSetChanged()
                    }
                }
            }
        })
        listItemView.findViewById<TextView>(R.id.system_notification_title).text = context.getString(typeTranslationMap.getValue(item.messageType))
        listItemView.findViewById<TextView>(R.id.system_notification_author).text = if (item.group.getType() != ManageableType.UNKNOWN) item.group.getName() else item.member.getName()
        listItemView.findViewById<TextView>(R.id.system_notification_date).text = DateFormat.getDateInstance().format(item.date)
        return listItemView
    }

}