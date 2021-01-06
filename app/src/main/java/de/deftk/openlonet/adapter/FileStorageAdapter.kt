package de.deftk.openlonet.adapter

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import de.deftk.lonet.api.implementation.OperatingScope
import de.deftk.lonet.api.model.IScope
import de.deftk.lonet.api.model.feature.Quota
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies

class FileStorageAdapter(context: Context, val elements: Map<OperatingScope, Quota>) : FilterableAdapter<OperatingScope>(context, elements.keys.toList()) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_file, parent, false)
        val item = getItem(position) ?: return listItemView

        listItemView.findViewById<TextView>(R.id.file_name).text = (item as IScope).name
        val quota = elements[item] ?: Quota(-1, -1, -1, -1, -1, -1)
        listItemView.findViewById<TextView>(R.id.file_size).text = String.format(context.getString(R.string.quota), Formatter.formatFileSize(context, quota.free), Formatter.formatFileSize(context, quota.limit))
        val imageView = listItemView.findViewById<ImageView>(R.id.file_image)
        imageView.setImageResource(R.drawable.ic_folder_24)

        return listItemView
    }

    override fun search(constraint: String?): List<OperatingScope> {
        if (constraint == null)
            return originalElements
        return originalElements.filter { it.filterApplies(constraint) }
    }

    override fun sort(elements: List<OperatingScope>): List<OperatingScope> {
        return elements.sortedWith(compareBy({ it.login != AuthStore.getApiUser().login }, { it.name }))
    }
}