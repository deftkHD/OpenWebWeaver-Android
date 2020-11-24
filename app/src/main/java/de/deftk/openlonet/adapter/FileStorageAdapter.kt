package de.deftk.openlonet.adapter

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import de.deftk.lonet.api.model.abstract.AbstractOperator
import de.deftk.lonet.api.model.abstract.IManageable
import de.deftk.lonet.api.model.feature.Quota
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies

class FileStorageAdapter(context: Context, val elements: Map<AbstractOperator, Quota>) : FilterableAdapter<AbstractOperator>(context, elements.keys.toList()) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_file, parent, false)
        val item = getItem(position) ?: return listItemView

        listItemView.findViewById<TextView>(R.id.file_name).text = (item as IManageable).getName()
        val quota = elements[item] ?: Quota(-1, -1, -1, -1, -1, -1)
        listItemView.findViewById<TextView>(R.id.file_size).text = String.format(context.getString(R.string.quota), Formatter.formatFileSize(context, quota.free), Formatter.formatFileSize(context, quota.limit))
        val imageView = listItemView.findViewById<ImageView>(R.id.file_image)
        imageView.setImageResource(R.drawable.ic_folder_24)

        return listItemView
    }

    override fun search(constraint: String?): List<AbstractOperator> {
        if (constraint == null)
            return originalElements
        return originalElements.filter { it.filterApplies(constraint) }
    }

    override fun sort(elements: List<AbstractOperator>): List<AbstractOperator> {
        return elements.sortedWith(compareBy({ it.getLogin() != AuthStore.getAppUser().getLogin() }, { it.getName() }))
    }
}