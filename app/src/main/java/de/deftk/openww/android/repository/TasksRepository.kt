package de.deftk.openww.android.repository

import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.tasks.ITask
import de.deftk.openww.android.api.Response
import java.util.*
import javax.inject.Inject

class TasksRepository @Inject constructor() : AbstractRepository() {

    suspend fun getTasks(apiContext: ApiContext): Response<List<Pair<ITask, IOperatingScope>>> = apiCall {
        apiContext.user.getAllTasks(apiContext).sortedByDescending { it.first.startDate?.time ?: it.first.created.date.time }
    }

    suspend fun addTask(title: String, completed: Boolean? = null, description: String? = null, dueDate: Long? = null, startDate: Long? = null, scope: IOperatingScope, apiContext: ApiContext) = apiCall {
        scope.addTask(
            title,
            completed,
            description,
            dueDate,
            startDate,
            scope.getRequestContext(apiContext)
        )
    }

    suspend fun editTask(task: ITask, title: String, completed: Boolean? = null, description: String? = null, dueDate: Date? = null, startDate: Date? = null, scope: IOperatingScope, apiContext: ApiContext) = apiCall {
        task.edit(
            title,
            description,
            completed,
            startDate,
            dueDate,
            scope.getRequestContext(apiContext)
        )
    }

    suspend fun deleteTask(task: ITask, scope: IOperatingScope, apiContext: ApiContext) = apiCall {
        task.delete(scope.getRequestContext(apiContext))
    }

}