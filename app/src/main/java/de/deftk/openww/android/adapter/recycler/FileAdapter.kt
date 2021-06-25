package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.ListItemFileBinding
import de.deftk.openww.android.fragments.feature.filestorage.FileClickHandler
import de.deftk.openww.android.fragments.feature.filestorage.FilesFragmentDirections
import de.deftk.openww.android.viewmodel.FileStorageViewModel
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.FilePreviewUrl
import de.deftk.openww.api.model.feature.filestorage.FileType
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile

class FileAdapter(private val scope: IOperatingScope, private val clickHandler: FileClickHandler, private val folderId: String?, private val path: Array<String>?, private val fileStorageViewModel: FileStorageViewModel) : ListAdapter<IRemoteFile, RecyclerView.ViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding, clickHandler)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val file = getItem(position)
        (holder as FileViewHolder).bind(
            scope,
            file,
            folderId,
            path,
            fileStorageViewModel.networkTransfers.value?.firstOrNull { it.id == file.id }?.progress,
            fileStorageViewModel.getLiveDataFromCache(scope, file.id, path?.toList())?.previewUrl
        )
    }

    public override fun getItem(position: Int): IRemoteFile {
        return super.getItem(position)
    }

    class FileViewHolder(val binding: ListItemFileBinding, private val clickHandler: FileClickHandler) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setClickListener { view ->
                if (binding.file!!.type == FileType.FOLDER) {
                    val path = if (binding.folderId != null) {
                        if (binding.path != null)
                            arrayOf(*binding.path!!, binding.folderId!!)
                        else arrayOf(binding.folderId!!)
                    } else null
                    val action = FilesFragmentDirections.actionFilesFragmentSelf(binding.file!!.id, binding.scope!!.login, binding.file!!.name, path)
                    view.findNavController().navigate(action)
                } else if (binding.file!!.type == FileType.FILE) {
                    clickHandler.onClick(view, this)
                }
            }
            itemView.setOnLongClickListener {
                itemView.showContextMenu()
                true
            }
        }

        fun bind(scope: IOperatingScope, file: IRemoteFile, folderId: String?, path: Array<String>?, progress: Int?, previewUrl: FilePreviewUrl?) {
            binding.scope = scope
            binding.file = file
            binding.folderId = folderId
            binding.path = path
            setProgress(progress ?: 0)
            if (previewUrl != null) {
                Glide.with(itemView.context)
                    .load(previewUrl.url)
                    .placeholder(R.drawable.ic_file_32)
                    .signature(ObjectKey(file.id))
                    .into(binding.fileImage)
            } else {
                Glide.with(itemView).clear(binding.fileImage)
            }
            binding.executePendingBindings()
        }

        private fun setProgress(progress: Int) {
            if (progress < 1 || progress >= 100) {
                binding.progressFile.isVisible = false
            } else {
                if (!binding.progressFile.isVisible)
                    binding.progressFile.isVisible = true
                binding.progressFile.progress = progress
            }
        }

    }

}

class FileDiffCallback : DiffUtil.ItemCallback<IRemoteFile>() {

    override fun areItemsTheSame(oldItem: IRemoteFile, newItem: IRemoteFile): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: IRemoteFile, newItem: IRemoteFile): Boolean {
        return oldItem.equals(newItem)
    }
}
