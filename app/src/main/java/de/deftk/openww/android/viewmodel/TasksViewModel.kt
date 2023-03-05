package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.filter.TaskFilter
import de.deftk.openww.android.repository.TasksRepository
import de.deftk.openww.android.room.IgnoredTask
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.tasks.ITask
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val tasksRepository: TasksRepository): ScopedViewModel(savedStateHandle) {

    private val _tasksResponse = registerProperty<Response<List<Pair<ITask, IOperatingScope>>>?>("tasksResponse", true)
    val allTasksResponse: LiveData<Response<List<Pair<ITask, IOperatingScope>>>?> = _tasksResponse

    private val _filter = registerProperty("filter", true, TaskFilter(tasksRepository.ignoredTaskDao))
    val filter: LiveData<TaskFilter> = _filter

    val filteredTasksResponse: LiveData<Response<List<Pair<ITask, IOperatingScope>>>?>
        get() = _filter.switchMap { filter ->
            when (filter) {
                null -> allTasksResponse
                else -> allTasksResponse.switchMap { response ->
                    val filtered = registerProperty<Response<List<Pair<ITask, IOperatingScope>>>?>("filtered", true)
                    filtered.postValue(response?.smartMap { filter.apply(it) })
                    filtered
                }
            }
        }

    private val _postResponse = registerProperty<Response<ITask?>?>("postResponse", true)
    val postResponse: LiveData<Response<ITask?>?> = _postResponse

    private val _batchDeleteResponse = registerProperty<List<Response<Pair<ITask, IOperatingScope>>>?>("batchDeleteResponse", true)
    val batchDeleteResponse: LiveData<List<Response<Pair<ITask, IOperatingScope>>>?> = _batchDeleteResponse

    fun loadTasks(includeIgnored: Boolean, apiContext: IApiContext) {
        viewModelScope.launch {
            _tasksResponse.postValue(tasksRepository.getTasks(includeIgnored, apiContext))
        }
    }

    fun addTask(title: String, description: String?, completed: Boolean?, startDate: Date?, dueDate: Date?, scope: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = tasksRepository.addTask(title, completed, description, dueDate?.time, startDate?.time, scope, apiContext)
            _postResponse.postValue(response)
            if (_tasksResponse.value is Response.Success && response is Response.Success) {
                // inject new task into stored livedata
                val tasks = (_tasksResponse.value as Response.Success<List<Pair<ITask, IOperatingScope>>>).value.toMutableList()
                tasks.add(response.value to scope)
                _tasksResponse.postValue(Response.Success(tasks))
            }
        }
    }

    fun editTask(task: ITask, title: String, description: String?, completed: Boolean?, startDate: Date?, dueDate: Date?, operator: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = tasksRepository.editTask(
                task,
                title,
                completed,
                description,
                dueDate,
                startDate,
                operator,
                apiContext
            )
            _postResponse.postValue(response.smartMap { task })
            if (response is Response.Success) {
                // no need to update items in list because the instance will be the same

                // trigger observers
                _tasksResponse.postValue(_tasksResponse.value)
            }
        }
    }

    fun deleteTask(task: ITask, operator: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = tasksRepository.deleteTask(task, operator, apiContext)
            _postResponse.postValue(Response.Success(task))
            if (response is Response.Success && _tasksResponse.value is Response.Success) {
                val tasks = (_tasksResponse.value as Response.Success<List<Pair<ITask, IOperatingScope>>>).value.toMutableList()
                tasks.remove(Pair(task, operator))
                _tasksResponse.postValue(Response.Success(tasks))
            }
        }
    }

    fun resetPostResponse() {
        _postResponse.postValue(null)
    }

    fun batchDelete(selectedTasks: List<Pair<ITask, IOperatingScope>>, apiContext: IApiContext) {
        viewModelScope.launch {
            val responses = selectedTasks.map { tasksRepository.deleteTask(it.first, it.second, apiContext) }
            _batchDeleteResponse.postValue(responses)
            val tasks = _tasksResponse.value?.valueOrNull()
            if (tasks != null) {
                val currentTasks = tasks.toMutableList()
                responses.forEach { response ->
                    if (response is Response.Success) {
                        currentTasks.remove(response.value)
                    }
                }
                _tasksResponse.postValue(Response.Success(currentTasks))
            }
        }
    }

    fun resetBatchDeleteResponse() {
        _batchDeleteResponse.postValue(null)
    }

    fun ignoreTasks(tasks: List<Pair<ITask, IOperatingScope>>, apiContext: IApiContext) {
        viewModelScope.launch {
            tasksRepository.ignoreTasks(tasks, apiContext)
            _tasksResponse.postValue(_tasksResponse.value)
        }
    }

    fun unignoreTasks(tasks: List<Pair<ITask, IOperatingScope>>, apiContext: IApiContext) {
        viewModelScope.launch {
            tasksRepository.unignoreTasks(tasks, apiContext)
            _tasksResponse.postValue(_tasksResponse.value)
        }
    }

    fun setFilter(setFilter: (filter: TaskFilter) -> Unit) {
        val filter = this._filter.value ?: TaskFilter(tasksRepository.ignoredTaskDao)
        setFilter(filter)
        this._filter.postValue(filter)
    }

    fun getIgnoredTasksBlocking(apiContext: IApiContext): List<IgnoredTask> {
        return runBlocking {
            tasksRepository.ignoredTaskDao.getIgnoredTasks(apiContext.user.login)
        }
    }

}