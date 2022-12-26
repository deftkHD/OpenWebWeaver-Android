package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import de.deftk.openww.android.databinding.ListItemExceptionBinding
import de.deftk.openww.android.feature.devtools.ExceptionReport
import de.deftk.openww.android.fragments.ActionModeClickListener

// maybe this whole thing could receive an actionmode menu, but until now I just implemented it this way because its easier
class ExceptionAdapter(clickListener: ActionModeClickListener<ReportViewHolder>): ActionModeAdapter<ExceptionReport, ExceptionAdapter.ReportViewHolder>(ExceptionDiffCallback(), clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ListItemExceptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        @Suppress("UNCHECKED_CAST")
        return ReportViewHolder(binding, clickListener as ActionModeClickListener<ActionModeViewHolder>)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val request = getItem(position)
        holder.bind(request)
    }

    class ReportViewHolder(val binding: ListItemExceptionBinding, clickListener: ActionModeClickListener<ActionModeViewHolder>): ActionModeViewHolder(binding.root, clickListener) {

        private var selected = false

        override fun isSelected(): Boolean {
            return selected
        }

        override fun setSelected(selected: Boolean) {
            this.selected = selected
            binding.selected = selected
        }

        fun bind(report: ExceptionReport) {
            val title = report.getTitle()
            binding.title = title
            binding.details = report.getDetail()
            binding.exceptionId = report.id
            binding.executePendingBindings()
        }
    }

}

class ExceptionDiffCallback : DiffUtil.ItemCallback<ExceptionReport>() {

    override fun areItemsTheSame(oldItem: ExceptionReport, newItem: ExceptionReport): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ExceptionReport, newItem: ExceptionReport): Boolean {
        return oldItem.equals(newItem)
    }
}