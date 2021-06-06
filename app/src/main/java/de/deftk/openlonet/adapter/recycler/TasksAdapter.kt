package de.deftk.openlonet.adapter.recycler

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.lonet.api.model.IOperatingScope
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.tasks.ITask
import de.deftk.openlonet.R
import de.deftk.openlonet.databinding.ListItemTaskBinding
import de.deftk.openlonet.fragments.feature.tasks.TasksFragmentDirections
import de.deftk.openlonet.utils.UIUtil
import java.util.*

class TasksAdapter : ListAdapter<Pair<ITask, IOperatingScope>, RecyclerView.ViewHolder>(TaskDiffCallback()) {

    companion object {

        @JvmStatic
        @BindingAdapter("app:strikeThroughTask")
        fun strikeThrough(view: TextView, task: ITask) {
            if (task.getEndDate() != null && Date().compareTo(task.getEndDate()) > -1) {
                view.paintFlags = view.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                view.paintFlags = view.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }

        @JvmStatic
        @BindingAdapter("app:taskDueDate")
        fun taskDueDate(view: TextView, task: ITask) {
            view.text = if (task.getEndDate() != null) UIUtil.getTaskDue(task) else view.context.getString(R.string.not_set)
        }

        @JvmStatic
        @BindingAdapter("app:taskCompleted")
        fun taskCompleted(view: ImageView, task: ITask) {
            view.setBackgroundResource(if (task.isCompleted()) R.drawable.ic_check_green_32 else 0)
        }

    }

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