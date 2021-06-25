package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.databinding.ListItemTaskBinding
import de.deftk.openww.android.fragments.feature.tasks.TasksFragmentDirections
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.tasks.ITask

class TasksAdapter : ListAdapter<Pair<ITask, IOperatingScope>, RecyclerView.ViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (task, scope) = getItem(position)
        (holder as TaskViewHolder).bind(task, scope)
    }

    public override fun getItem(position: Int): Pair<ITask, IOperatingScope> {
        return super.getItem(position)
    }

    class TaskViewHolder(val binding: ListItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setClickListener { view ->
                view.findNavController().navigate(TasksFragmentDirections.actionTasksFragmentToReadTaskFragment(binding.task!!.id, binding.scope!!.login))
            }
            itemView.setOnLongClickListener {
                itemView.showContextMenu()
                true
            }
        }

        fun bind(task: ITask, scope: IOperatingScope) {
            binding.task = task
            binding.scope = scope
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