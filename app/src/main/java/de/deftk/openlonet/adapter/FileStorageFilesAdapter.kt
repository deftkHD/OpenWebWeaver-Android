package de.deftk.openlonet.adapter

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import de.deftk.lonet.api.implementation.OperatingScope
import de.deftk.lonet.api.model.feature.filestorage.FileType
import de.deftk.lonet.api.model.feature.filestorage.IRemoteFile
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileStorageFilesAdapter(context: Context, elements: List<IRemoteFile>, private val operator: OperatingScope) :
    FilterableAdapter<IRemoteFile>(context, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_file, parent, false)
        val item = getItem(position) ?: return listItemView

        listItemView.findViewById<TextView>(R.id.file_name).text = item.name
        listItemView.findViewById<TextView>(R.id.file_size).text = if (item.type == FileType.FILE) Formatter.formatFileSize(context, item.getSize()) else context.getString(R.string.directory)
        listItemView.findViewById<TextView>(R.id.file_modified_date).text = TextUtils.parseShortDate(item.getModified().date)
        val imageView = listItemView.findViewById<ImageView>(R.id.file_image)
        when (item.type) {
            FileType.FILE -> {
                if (item.hasPreview() == true) {
                    CoroutineScope(Dispatchers.IO).launch {
                        withContext(Dispatchers.IO) {
                            val url = item.getPreviewUrl(operator.getRequestContext(AuthStore.getApiContext())).url
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
            FileType.FOLDER -> imageView.setImageResource(R.drawable.ic_folder_32)
            else -> imageView.setImageDrawable(null)
        }



        return listItemView
    }

    override fun search(constraint: String?): List<IRemoteFile> {
        if (constraint == null)
            return originalElements
        return originalElements.filter {
            it.created.member.filterApplies(constraint)
                    || it.name.filterApplies(constraint)
                    || it.getDescription().filterApplies(constraint)
        }
    }

    override fun sort(elements: List<IRemoteFile>): List<IRemoteFile> {
        return elements.sortedWith(compareBy({ it.type == FileType.FILE }, { it.name }))
    }
}