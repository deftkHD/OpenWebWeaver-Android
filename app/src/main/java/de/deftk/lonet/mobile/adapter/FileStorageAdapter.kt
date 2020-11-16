package de.deftk.lonet.mobile.adapter

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import de.deftk.lonet.api.model.abstract.IManageable
import de.deftk.lonet.api.model.feature.Quota
import de.deftk.lonet.api.model.feature.abstract.IFileStorage
import de.deftk.lonet.mobile.R

class FileStorageAdapter(context: Context, val elements: Map<IFileStorage, Quota>) : ArrayAdapter<IFileStorage>(context, 0, elements.keys.toList()) {

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

}