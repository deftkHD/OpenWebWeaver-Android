package de.deftk.lonet.mobile.feature.overview

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.deftk.lonet.api.model.feature.Quota
import de.deftk.lonet.mobile.R

class FileStorageOverview(val quota: Quota): AbstractOverviewElement(object : AbstractOverviewAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup, context: Context): View {
        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_overview_file_storage, parent, false)
        itemView.findViewById<TextView>(R.id.overview_file_quota).text = String.format(context.getString(R.string.quota), Formatter.formatFileSize(context, quota.free), Formatter.formatFileSize(context, quota.limit))
        return itemView
    }
})