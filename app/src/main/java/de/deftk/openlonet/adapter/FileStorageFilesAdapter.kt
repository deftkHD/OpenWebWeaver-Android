package de.deftk.openlonet.adapter

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import de.deftk.lonet.api.model.feature.files.OnlineFile
import de.deftk.openlonet.R
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileStorageFilesAdapter(context: Context, elements: List<OnlineFile>) :
    FilterableAdapter<OnlineFile>(context, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_file, parent, false)
        val item = getItem(position) ?: return listItemView

        listItemView.findViewById<TextView>(R.id.file_name).text = item.name
        listItemView.findViewById<TextView>(R.id.file_size).text = if (item.type == OnlineFile.FileType.FILE) Formatter.formatFileSize(context, item.size) else context.getString(R.string.directory)
        listItemView.findViewById<TextView>(R.id.file_modified_date).text = TextUtils.parseShortDate(item.modificationDate)
        val imageView = listItemView.findViewById<ImageView>(R.id.file_image)
        when (item.type) {
            OnlineFile.FileType.FILE -> {
                if (item.preview == true) {
                    CoroutineScope(Dispatchers.IO).launch {
                        withContext(Dispatchers.IO) {
                            val url = item.getPreviewDownloadUrl().downloadUrl
                            withContext(Dispatchers.Main) {
                                Glide.with(listItemView)
                                    .load(url)
                                    .placeholder(R.drawable.ic_file_32)
                                    .optionalFitCenter()
                                    .into(imageView)
                            }

                        }
                    }
                } else {
                    imageView.setImageResource(R.drawable.ic_file_32)
                }
            }
            OnlineFile.FileType.FOLDER -> imageView.setImageResource(R.drawable.ic_folder_32)
            else -> imageView.setImageDrawable(null)
        }



        return listItemView
    }

    override fun search(constraint: String?): List<OnlineFile> {
        if (constraint == null)
            return originalElements
        return originalElements.filter {
            it.creationMember.filterApplies(constraint)
                    || it.name.filterApplies(constraint)
                    || it.description.filterApplies(constraint)
        }
    }

    override fun sort(elements: List<OnlineFile>): List<OnlineFile> {
        return elements.sortedWith(compareBy({ it.type == OnlineFile.FileType.FILE }, { it.name }))
    }
}