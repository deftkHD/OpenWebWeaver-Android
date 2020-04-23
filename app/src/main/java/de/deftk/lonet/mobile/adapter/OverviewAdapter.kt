package de.deftk.lonet.mobile.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import de.deftk.lonet.mobile.feature.overview.AbstractOverviewElement

class OverviewAdapter(context: Context, elements: List<AbstractOverviewElement>): ArrayAdapter<AbstractOverviewElement>(context, 0, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position) ?: return super.getView(position, convertView, parent)
        return item.adapter.getView(position, convertView, parent, context)
    }

}