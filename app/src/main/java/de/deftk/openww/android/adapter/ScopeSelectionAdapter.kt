package de.deftk.openww.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import de.deftk.openww.android.R
import de.deftk.openww.api.model.IOperatingScope

class ScopeSelectionAdapter(context: Context, elements: List<IOperatingScope>): ArrayAdapter<IOperatingScope>(context, 0, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.support_simple_spinner_dropdown_item, parent, false)
        val item = getItem(position) ?: return listItemView
        (listItemView as TextView).text = item.name
        return listItemView
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }

}