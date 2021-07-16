package de.deftk.openww.android.filter

import de.deftk.openww.android.R
import de.deftk.openww.android.room.IgnoredTaskDao
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.tasks.ITask
import kotlinx.coroutines.runBlocking
import java.util.*

class TaskFilter(private val ignoredTaskDao: IgnoredTaskDao) : Filter<Pair<ITask, IOperatingScope>>(TaskOrder.ByGivenDesc) {

    private val groupDelegate = ScopeFilter()
    private val creatorDelegate = ScopeFilter()

    var account: String? = null

    val titleCriteria = addCriteria<String>(R.string.name, null) { element, value ->
        value ?: return@addCriteria true
        element.first.title.contains(value, true)
    }

    val descriptionCriteria = addCriteria<String>(R.string.description, null) { element, value ->
        value ?: return@addCriteria true
        element.first.description?.contains(value, true) == true
    }

    val showIgnoredCriteria = addCriteria<Boolean>(R.string.ignore, null) { element, value ->
        account ?: return@addCriteria true
        val isIgnored = runBlocking { ignoredTaskDao.getIgnoredTasks(account!!).any { it.id == element.first.id && it.scope == element.second.login } }
        (value ?: false) || !isIgnored
    }

    val smartSearchCriteria = addCriteria<String>(R.string.smart_search, null) { element, value ->
        value ?: return@addCriteria true
        titleCriteria.matches(element, value)
                || descriptionCriteria.matches(element, value)
                || groupDelegate.nameCriteria.matches(element.second, value)
                || creatorDelegate.loginCriteria.matches(element.first.created.member, value)
                || creatorDelegate.nameCriteria.matches(element.first.created.member, value)
    }


}

