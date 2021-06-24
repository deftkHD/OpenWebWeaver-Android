package de.deftk.openww.android.adapter.recycler

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.Quota
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.ListItemFileStorageBinding
import de.deftk.openww.android.fragments.feature.filestorage.FileStorageGroupFragmentDirections

class FileStorageAdapter : ListAdapter<Pair<IOperatingScope, Quota>, RecyclerView.ViewHolder>(FileStorageDiffCallback()) {

    companion object {

        @JvmStatic
        @BindingAdapter("app:quotaProgress")
        fun quotaProgress(view: ProgressBar, quota: Quota) {
            view.progress = ((quota.usage.toFloat() / quota.limit) * 100).toInt()
        }

        @JvmStatic
        @BindingAdapter("app:quotaText")
        fun quotaText(view: TextView, quota: Quota) {
            view.text = String.format(view.context.getString(R.string.quota), Formatter.formatFileSize(view.context, quota.free), Formatter.formatFileSize(view.context, quota.limit))
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemFileStorageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileStorageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (scope, quota) = getItem(position)
        (holder as FileStorageViewHolder).bind(scope, quota)
    }

    class FileStorageViewHolder(val binding: ListItemFileStorageBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setClickListener { view ->
                view.findNavController().navigate(FileStorageGroupFragmentDirections.actionFileStorageGroupFragmentToFilesFragment(null, binding.scope!!.login, binding.scope!!.name))
            }
        }

        fun bind(scope: IOperatingScope, quota: Quota) {
            binding.scope = scope
            binding.quota = quota
            binding.executePendingBindings()
        }

    }

}

class FileStorageDiffCallback : DiffUtil.ItemCallback<Pair<IOperatingScope, Quota>>() {

    override fun areItemsTheSame(oldItem: Pair<IOperatingScope, Quota>, newItem: Pair<IOperatingScope, Quota>): Boolean {
        return oldItem.first.login == newItem.first.login && oldItem.second == newItem.second
    }

    override fun areContentsTheSame(oldItem: Pair<IOperatingScope, Quota>, newItem: Pair<IOperatingScope, Quota>): Boolean {
        return oldItem.first.login == newItem.first.login && oldItem.second.equals(newItem.second)
    }
}