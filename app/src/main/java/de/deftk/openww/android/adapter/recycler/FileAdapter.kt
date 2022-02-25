package de.deftk.openww.android.adapter.recycler

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.ListItemFileBinding
import de.deftk.openww.android.fragments.ActionModeClickListener
import de.deftk.openww.android.viewmodel.FileStorageViewModel
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.FilePreviewUrl
import de.deftk.openww.api.model.feature.filestorage.FileType
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import java.util.*

class FileAdapter(
    var scope: IOperatingScope,
    clickListener: ActionModeClickListener<FileViewHolder>,
    private val fileStorageViewModel: FileStorageViewModel
) : ActionModeAdapter<IRemoteFile, FileAdapter.FileViewHolder>(FileDiffCallback(), clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ListItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding, clickListener as ActionModeClickListener<ActionModeViewHolder>)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = getItem(position)
        holder.bind(
            scope,
            file,
            fileStorageViewModel.networkTransfers.value?.firstOrNull { it.id == file.id }?.progress,
            fileStorageViewModel.getAllFiles(scope).value?.valueOrNull()?.firstOrNull { it.file.id == file.id }?.previewUrl
        )
    }

    class FileViewHolder(val binding: ListItemFileBinding, clickListener: ActionModeClickListener<ActionModeViewHolder>) : ActionModeViewHolder(binding.root, clickListener) {

        private var selected: Boolean = false

        init {
            binding.setMenuClickListener {
                it.showContextMenu()
            }
        }

        override fun isSelected(): Boolean {
            return selected
        }

        override fun setSelected(selected: Boolean) {
            this.selected = selected
            binding.selected = selected
        }

        fun bind(scope: IOperatingScope, file: IRemoteFile, progress: Int?, previewUrl: FilePreviewUrl?) {
            binding.selected = false
            binding.scope = scope
            binding.file = file
            binding.readable = file.effectiveRead == true
            val recentlyCreated = Date().time - file.created.date.time <= 259200000 // 3 days
            binding.recentlyCreated = recentlyCreated
            if (file.type == FileType.FOLDER && !recentlyCreated) {
                val recentlyAdded = file.aggregation?.newestFile?.created?.date?.time
                if (recentlyAdded != null)
                    binding.recentlyCreated = Date().time - recentlyAdded <= 259200000 // 3 days
            }
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

        //TODO don't set progress but rather supply current value and maximum -> also update size label
        @SuppressLint("SetTextI18n")
        fun setProgress(progress: Int) {
            if (progress < 1 || progress >= 100) {
                binding.progressFile.isVisible = false
                binding.fileImage.visibility = View.VISIBLE
                binding.fileNewIndicator.isVisible = binding.recentlyCreated!!
            } else {
                if (!binding.progressFile.isVisible) {
                    binding.progressFile.isVisible = true
                    binding.fileImage.visibility = View.INVISIBLE
                    binding.fileNewIndicator.isVisible = false
                }
                binding.progressFile.progress = progress
                binding.fileSize.text = "$progress%"
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
