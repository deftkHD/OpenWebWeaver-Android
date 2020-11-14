package de.deftk.lonet.mobile.adapter

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import de.deftk.lonet.api.model.feature.files.OnlineFile
import de.deftk.lonet.mobile.R
import java.text.DateFormat

class FileStorageFilesAdapter(context: Context, elements: List<OnlineFile>) :
    ArrayAdapter<OnlineFile>(context, 0, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_file, parent, false)
        val item = getItem(position) ?: return listItemView

        listItemView.findViewById<TextView>(R.id.file_name).text = item.name
        listItemView.findViewById<TextView>(R.id.file_size).text = if (item.type == OnlineFile.FileType.FILE) Formatter.formatFileSize(context, item.size) else context.getString(R.string.directory)
        listItemView.findViewById<TextView>(R.id.file_modified_date).text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(item.modificationDate)
        val imageView = listItemView.findViewById<ImageView>(R.id.file_image)
        when (item.type) {
            OnlineFile.FileType.FILE -> imageView.setImageResource(R.drawable.ic_file_dark_24)
            OnlineFile.FileType.FOLDER -> imageView.setImageResource(R.drawable.ic_folder_dark_24)
            else -> imageView.setImageDrawable(null)
        }
        return listItemView
    }

}