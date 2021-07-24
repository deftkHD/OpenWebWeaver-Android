package de.deftk.openww.android.feature.overview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import de.deftk.openww.android.R

class TasksOverview(val tasksDone: Int, val tasksAll: Int) : AbstractOverviewElement(object : AbstractOverviewAdapter() {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup, context: Context): View {
            val itemView = LayoutInflater.from(context).inflate(R.layout.list_item_overview_tasks, parent, false)
            itemView.findViewById<TextView>(R.id.overview_tasks_done).text = String.format(context.getString(R.string.tasks_done), tasksDone, tasksAll)
            itemView.findViewById<ProgressBar>(R.id.progress_tasks_done).progress = ((tasksDone.toFloat() / tasksAll) * 100).toInt()
            return itemView
        }

    })