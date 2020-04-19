package de.deftk.lonet.mobile.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.feature.overview.AbstractOverviewElement
import de.deftk.lonet.mobile.feature.overview.FileStorageOverview
import de.deftk.lonet.mobile.feature.overview.MailOverview
import de.deftk.lonet.mobile.feature.overview.SystemNotificationsOverview
import de.deftk.lonet.mobile.utils.UnitUtil

class OverviewAdapter(context: Context, elements: List<AbstractOverviewElement>): ArrayAdapter<AbstractOverviewElement>(context, 0, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position) ?: return super.getView(position, convertView, parent)
        return item.adapter.getView(position, convertView, parent, context)
    }

}