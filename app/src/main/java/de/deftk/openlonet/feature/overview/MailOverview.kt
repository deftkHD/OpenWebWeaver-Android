package de.deftk.openlonet.feature.overview

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.deftk.lonet.api.model.feature.Quota
import de.deftk.openlonet.R

class MailOverview(val quota: Quota, val unreadMails: Int): AbstractOverviewElement(object : AbstractOverviewAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup, context: Context): View {
        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_overview_mail, parent, false)
        itemView.findViewById<TextView>(R.id.overview_mail_count).text = String.format(context.getString(R.string.unread), unreadMails)
        itemView.findViewById<TextView>(R.id.overview_mail_quota).text = String.format(context.getString(R.string.quota), Formatter.formatFileSize(context, quota.free), Formatter.formatFileSize(context, quota.limit))
        return itemView
    }
})