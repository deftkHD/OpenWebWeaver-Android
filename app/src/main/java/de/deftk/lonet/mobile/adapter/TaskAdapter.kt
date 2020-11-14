package de.deftk.lonet.mobile.adapter

import android.content.Context
import android.graphics.Paint
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import de.deftk.lonet.api.model.feature.Task
import de.deftk.lonet.mobile.R
import java.util.*

class TaskAdapter(context: Context, tasks: List<Task>): ArrayAdapter<Task>(context, 0, tasks.toMutableList()) {

    private val dateFormat = DateFormat.getDateFormat(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_task, parent, false)
        val task = getItem(position)
        if (task != null) {
            // title
            val titleView = listItemView.findViewById<TextView>(R.id.task_title)
            titleView.text = task.title
            if (task.endDate != null && Date().compareTo(task.endDate) > -1)
                titleView.paintFlags = titleView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else
                titleView.paintFlags = titleView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            // author
            listItemView.findViewById<TextView>(R.id.task_author).text = task.creationMember.getName()
            // completed
            if (task.completed)
                listItemView.findViewById<ImageView>(R.id.task_completed).setImageResource(R.drawable.ic_check_green_32)
            else
                listItemView.findViewById<ImageView>(R.id.task_completed).setImageDrawable(null)
            // due date
            listItemView.findViewById<TextView>(R.id.task_due).text = String.format(context.getString(R.string.until_date), if (task.endDate != null) dateFormat.format(task.endDate!!) else context.getString(R.string.not_set))
        }
        return listItemView
    }
}