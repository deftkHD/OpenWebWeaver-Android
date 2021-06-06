package de.deftk.openlonet.adapter

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import de.deftk.lonet.api.model.IOperatingScope
import de.deftk.lonet.api.model.IUser
import de.deftk.lonet.api.model.feature.Quota
import de.deftk.openlonet.R
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies

class FileStorageAdapter(context: Context, val elements: Map<IOperatingScope, Quota>) : FilterableAdapter<Pair<IOperatingScope, Quota>>(context, elements.toList()) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_file, parent, false)
        val item = getItem(position)?.first ?: return listItemView

        listItemView.findViewById<TextView>(R.id.file_name).text = item.name
        val quota = elements[item] ?: Quota(-1, -1, -1, -1, -1, -1)
        listItemView.findViewById<TextView>(R.id.file_size).text = String.format(context.getString(R.string.quota), Formatter.formatFileSize(context, quota.free), Formatter.formatFileSize(context, quota.limit))
        val imageView = listItemView.findViewById<ImageView>(R.id.file_image)
        imageView.setImageResource(R.drawable.ic_folder_24)

        val progressBar = listItemView.findViewById<ProgressBar>(R.id.progress_file)
        progressBar.progress = ((quota.usage.toFloat() / quota.limit) * 100).toInt()
        progressBar.visibility = View.VISIBLE

        return listItemView
    }

    override fun search(constraint: String?): List<Pair<IOperatingScope, Quota>> {
        if (constraint == null)
            return originalElements
        return originalElements.filter { it.first.filterApplies(constraint) }
    }

    override fun sort(elements: List<Pair<IOperatingScope, Quota>>): List<Pair<IOperatingScope, Quota>> {
        return elements.sortedWith(compareBy({ it !is IUser }, { it.first.name }))
    }
}