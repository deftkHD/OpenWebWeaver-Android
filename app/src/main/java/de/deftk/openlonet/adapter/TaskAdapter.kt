package de.deftk.openlonet.adapter

import android.content.Context
import android.graphics.Paint
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import de.deftk.lonet.api.implementation.OperatingScope
import de.deftk.lonet.api.implementation.feature.tasks.Task
import de.deftk.openlonet.R
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies
import java.util.*

class TaskAdapter(context: Context, tasks: List<Pair<Task, OperatingScope>>): FilterableAdapter<Pair<Task, OperatingScope>>(context, tasks.toMutableList()) {

    private val dateFormat = DateFormat.getDateFormat(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_task, parent, false)
        val item = getItem(position)
        val task = item?.first
        if (task != null) {
            // title
            val titleView = listItemView.findViewById<TextView>(R.id.task_title)
            titleView.text = task.getTitle()
            if (task.getEndDate() != null && Date().compareTo(task.getEndDate()) > -1)
                titleView.paintFlags = titleView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else
                titleView.paintFlags = titleView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            // author
            listItemView.findViewById<TextView>(R.id.task_author).text = task.created.member.name
            // completed
            if (task.isCompleted())
                listItemView.findViewById<ImageView>(R.id.task_completed).setImageResource(R.drawable.ic_check_green_32)
            else
                listItemView.findViewById<ImageView>(R.id.task_completed).setImageDrawable(null)
            // due date
            listItemView.findViewById<TextView>(R.id.task_due).text = String.format(context.getString(R.string.until_date), if (task.getEndDate() != null) dateFormat.format(task.getEndDate()!!) else context.getString(R.string.not_set))
        }
        return listItemView
    }

    override fun search(constraint: String?): List<Pair<Task, OperatingScope>> {
        if (constraint == null)
            return originalElements
        return originalElements.filter {
            it.first.getTitle().contains(constraint, true)
                    || it.first.getDescription()?.contains(constraint, true) == true
                    || it.first.created.member.filterApplies(constraint)
        }
    }

    override fun sort(elements: List<Pair<Task, OperatingScope>>): List<Pair<Task, OperatingScope>> {
        return elements.sortedByDescending { it.first.created.date }
    }
}