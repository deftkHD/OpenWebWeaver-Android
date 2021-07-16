package de.deftk.openww.android.utils

import android.content.Intent
import android.provider.CalendarContract
import de.deftk.openww.api.model.feature.tasks.ITask

object CalendarUtil {

    fun importTaskIntoCalendar(task: ITask): Intent {
        val intent = Intent(Intent.ACTION_INSERT)
        intent.data = CalendarContract.Events.CONTENT_URI
        intent.putExtra(CalendarContract.Events.TITLE, task.title)
        if (task.startDate != null)
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, task.startDate!!.time)
        if (task.dueDate != null)
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, task.dueDate!!.time)
        if (task.description != null)
            intent.putExtra(CalendarContract.Events.DESCRIPTION, task.description)
        return intent
    }

}