package de.deftk.lonet.mobile.feature.overview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.deftk.lonet.api.model.feature.Quota
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.fragments.FileStorageFragment
import de.deftk.lonet.mobile.utils.UnitUtil

class FileStorageOverview(val quota: Quota): AbstractOverviewElement(object : AbstractOverviewAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup, context: Context): View {
        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_overview_file_storage, parent, false)
        itemView.findViewById<TextView>(R.id.overview_file_quota).text = String.format(context.getString(R.string.quota), UnitUtil.getFormattedSize(quota.free), UnitUtil.getFormattedSize(quota.limit))
        return itemView
    }
})