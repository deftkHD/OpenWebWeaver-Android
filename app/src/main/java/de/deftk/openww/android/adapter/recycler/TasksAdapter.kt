package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.ListItemTaskBinding
import de.deftk.openww.android.fragments.ActionModeClickListener
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.tasks.ITask

class TasksAdapter(clickListener: ActionModeClickListener<TaskViewHolder>) : ActionModeAdapter<Pair<ITask, IOperatingScope>, TasksAdapter.TaskViewHolder>(TaskDiffCallback(), clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ListItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        @Suppress("UNCHECKED_CAST")
        return TaskViewHolder(binding, clickListener as ActionModeClickListener<ActionModeViewHolder>)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val (task, scope) = getItem(position)
        holder.bind(task, scope)
    }

    class TaskViewHolder(val binding: ListItemTaskBinding, clickListener: ActionModeClickListener<ActionModeViewHolder>) : ActionModeViewHolder(binding.root, clickListener) {

        private var selected = false

        init {
            binding.setMenuClickListener {
                itemView.showContextMenu()
            }
        }

        override fun isSelected(): Boolean {
            return selected
        }

        override fun setSelected(selected: Boolean) {
            this.selected = selected
            binding.selected = selected
        }

        fun bind(task: ITask, scope: IOperatingScope) {
            binding.task = task
            binding.scope = scope
            binding.taskCompleted.contentDescription = itemView.context.getString(if (task.completed) R.string.task_completed_desc else R.string.task_not_completed_desc)
            binding.executePendingBindings()
        }

    }

}

class TaskDiffCallback: DiffUtil.ItemCallback<Pair<ITask, IOperatingScope>>() {

    override fun areItemsTheSame(oldItem: Pair<ITask, IOperatingScope>, newItem: Pair<ITask, IOperatingScope>): Boolean {
        return oldItem.first.id == newItem.first.id && oldItem.second.login == newItem.second.login
    }

    override fun areContentsTheSame(oldItem: Pair<ITask, IOperatingScope>, newItem: Pair<ITask, IOperatingScope>): Boolean {
        return oldItem.first.equals(newItem.first) && oldItem.second.login == newItem.second.login
    }
}