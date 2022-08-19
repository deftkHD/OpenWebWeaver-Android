package de.deftk.openww.android.adapter.recycler

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
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
import kotlin.math.roundToInt

class FileAdapter(
    var scope: IOperatingScope,
    clickListener: ActionModeClickListener<FileViewHolder>,
    private val fileStorageViewModel: FileStorageViewModel
) : ActionModeAdapter<IRemoteFile, FileAdapter.FileViewHolder>(FileDiffCallback(), clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ListItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        @Suppress("UNCHECKED_CAST")
        return FileViewHolder(binding, clickListener as ActionModeClickListener<ActionModeViewHolder>)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = getItem(position)
        val transfer = fileStorageViewModel.networkTransfers.value?.firstOrNull { it.id == file.id }
        holder.bind(
            scope,
            file,
            transfer?.progressValue,
            transfer?.maxProgress,
            fileStorageViewModel.getAllFiles(scope).value?.valueOrNull()?.firstOrNull { it.file.id == file.id }?.previewUrl,
            holder.itemView.context
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

        fun bind(scope: IOperatingScope, file: IRemoteFile, progressValue: Int?, maxProgress: Int?, previewUrl: FilePreviewUrl?, context: Context) {
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
            setProgress(progressValue ?: 0, maxProgress ?: 1, context) // do not divide by 0
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

        @SuppressLint("SetTextI18n")
        fun setProgress(value: Int, max: Int, context: Context) {
            if (max == 1)
                return
            val progress = (value.toFloat() / max.toFloat()) * 100
            if (value < 1 || value >= max) {
                binding.progressFile.isVisible = false
                binding.fileImage.visibility = View.VISIBLE
                binding.fileNewIndicator.isVisible = binding.recentlyCreated!!
                binding.fileSize.text = Formatter.formatFileSize(context, max.toLong())
            } else {
                if (!binding.progressFile.isVisible) {
                    binding.progressFile.isVisible = true
                    binding.fileImage.visibility = View.INVISIBLE
                    binding.fileNewIndicator.isVisible = false
                }
                binding.progressFile.progress = progress.toInt()
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("file_storage_display_progress_pct", false)) {
                    binding.fileSize.text = "${(progress * 100).roundToInt() / 100.0}%"
                } else {
                    binding.fileSize.text = "${Formatter.formatFileSize(context, value.toLong())}/${Formatter.formatFileSize(context, max.toLong())}"
                }

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
