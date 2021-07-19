package de.deftk.openww.android.repository

import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.tasks.ITask
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.room.IgnoredTask
import de.deftk.openww.android.room.IgnoredTaskDao
import de.deftk.openww.api.model.IApiContext
import java.util.*
import javax.inject.Inject

class TasksRepository @Inject constructor(val ignoredTaskDao: IgnoredTaskDao) : AbstractRepository() {

    suspend fun getTasks(includeIgnored: Boolean, apiContext: IApiContext): Response<List<Pair<ITask, IOperatingScope>>> = apiCall {
        val remoteTasks = apiContext.user.getAllTasks(apiContext).toMutableList()
        val ignoredTasks = ignoredTaskDao.getIgnoredTasks(apiContext.user.login)
        val unusedIgnoredTasks = mutableListOf<IgnoredTask>()
        ignoredTasks.forEach { ignoredTask ->
            val target = remoteTasks.filter { it.first.id == ignoredTask.id && it.second.login == ignoredTask.scope }
            if (target.isNotEmpty()) {
                if (!includeIgnored)
                    remoteTasks.removeAll(target)
            } else {
                unusedIgnoredTasks.add(ignoredTask)
            }
        }
        if (unusedIgnoredTasks.isNotEmpty()) {
            ignoredTaskDao.unignoreTasks(unusedIgnoredTasks)
        }
        remoteTasks
    }

    suspend fun addTask(title: String, completed: Boolean? = null, description: String? = null, dueDate: Long? = null, startDate: Long? = null, scope: IOperatingScope, apiContext: IApiContext) = apiCall {
        scope.addTask(
            title,
            completed,
            description,
            dueDate,
            startDate,
            scope.getRequestContext(apiContext)
        )
    }

    suspend fun editTask(task: ITask, title: String, completed: Boolean? = null, description: String? = null, dueDate: Date? = null, startDate: Date? = null, scope: IOperatingScope, apiContext: IApiContext) = apiCall {
        task.edit(
            title,
            description,
            completed,
            startDate,
            dueDate,
            scope.getRequestContext(apiContext)
        )
    }

    suspend fun deleteTask(task: ITask, scope: IOperatingScope, apiContext: IApiContext) = apiCall {
        task.delete(scope.getRequestContext(apiContext))
        task to scope
    }

    suspend fun ignoreTasks(tasks: List<Pair<ITask, IOperatingScope>>, apiContext: IApiContext) {
        ignoredTaskDao.ignoreTasks(tasks.map { IgnoredTask.from(apiContext.user.login, it.first, it.second) })
    }

    suspend fun unignoreTasks(tasks: List<Pair<ITask, IOperatingScope>>, apiContext: IApiContext) {
        ignoredTaskDao.unignoreTasks(tasks.map { IgnoredTask.from(apiContext.user.login, it.first, it.second) })
    }

}