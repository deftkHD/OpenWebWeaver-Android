package de.deftk.openww.android.feature.overview

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import de.deftk.openww.api.model.feature.Quota
import de.deftk.openww.android.R

class MailOverview(val quota: Quota, val unreadMails: Int) : AbstractOverviewElement(object : AbstractOverviewAdapter() {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup, context: Context): View {
            val itemView = LayoutInflater.from(context).inflate(R.layout.list_item_overview_mail, parent, false)
            itemView.findViewById<TextView>(R.id.overview_mail_count).text = String.format(context.getString(R.string.unread), unreadMails)
            itemView.findViewById<TextView>(R.id.overview_mail_quota).text = String.format(context.getString(R.string.quota), Formatter.formatFileSize(context, quota.free), Formatter.formatFileSize(context, quota.limit))
            itemView.findViewById<ProgressBar>(R.id.progress_mail_quota).progress = ((quota.usage.toFloat() / quota.limit) * 100).toInt()
            return itemView
        }
    })