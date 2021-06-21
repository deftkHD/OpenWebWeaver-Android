package de.deftk.openww.android.filter

import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.tasks.ITask
import de.deftk.openww.android.R
import java.util.*

class TaskFilter : Filter<Pair<ITask, IOperatingScope>>(TaskOrder.ByDueDateAsc) {

    val titleCriteria = addCriteria<String>(R.string.name, null) { element, value ->
        if (value == null)
            return@addCriteria true
        element.first.getTitle().toLowerCase(Locale.getDefault()).startsWith(value.toLowerCase(Locale.getDefault()))
    }


}

