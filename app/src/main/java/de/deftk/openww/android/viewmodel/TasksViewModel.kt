package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.filter.TaskFilter
import de.deftk.openww.android.repository.TasksRepository
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.tasks.ITask
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val tasksRepository: TasksRepository): ViewModel() {

    private val _tasksResponse = MutableLiveData<Response<List<Pair<ITask, IOperatingScope>>>>()
    val allTasksResponse: LiveData<Response<List<Pair<ITask, IOperatingScope>>>> = _tasksResponse

    var filter = MutableLiveData<TaskFilter?>(TaskFilter())

    val filteredTasksResponse: LiveData<Response<List<Pair<ITask, IOperatingScope>>>>
        get() = filter.switchMap { filter ->
            when (filter) {
                null -> allTasksResponse
                else -> allTasksResponse.switchMap { response ->
                    val filtered = MutableLiveData<Response<List<Pair<ITask, IOperatingScope>>>>()
                    filtered.value = response.smartMap { filter.apply(it) }
                    filtered
                }
            }
        }

    private val _postResponse = MutableLiveData<Response<ITask?>?>()
    val postResponse: LiveData<Response<ITask?>?> = _postResponse

    private val _batchDeleteResponse = MutableLiveData<List<Response<Pair<ITask, IOperatingScope>>>?>()
    val batchDeleteResponse: LiveData<List<Response<Pair<ITask, IOperatingScope>>>?> = _batchDeleteResponse

    fun loadTasks(apiContext: ApiContext) {
        viewModelScope.launch {
            _tasksResponse.value = tasksRepository.getTasks(apiContext)
        }
    }

    fun addTask(title: String, description: String?, completed: Boolean?, startDate: Date?, dueDate: Date?, scope: IOperatingScope, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = tasksRepository.addTask(title, completed, description, dueDate?.time, startDate?.time, scope, apiContext)
            _postResponse.value = response
            if (_tasksResponse.value is Response.Success && response is Response.Success) {
                // inject new task into stored livedata
                val tasks = (_tasksResponse.value as Response.Success<List<Pair<ITask, IOperatingScope>>>).value.toMutableList()
                tasks.add(response.value to scope)
                _tasksResponse.value = Response.Success(tasks)
            }
        }
    }

    fun editTask(task: ITask, title: String, description: String?, completed: Boolean?, startDate: Date?, dueDate: Date?, operator: IOperatingScope, apiContext: ApiContext) {
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
            _postResponse.value = response.smartMap { task }
            if (response is Response.Success) {
                // no need to update items in list because the instance will be the same

                // trigger observers
                _tasksResponse.value = _tasksResponse.value
            }
        }
    }

    fun deleteTask(task: ITask, operator: IOperatingScope, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = tasksRepository.deleteTask(task, operator, apiContext)
            _postResponse.value = Response.Success(task)
            if (response is Response.Success && _tasksResponse.value is Response.Success) {
                val tasks = (_tasksResponse.value as Response.Success<List<Pair<ITask, IOperatingScope>>>).value.toMutableList()
                tasks.remove(Pair(task, operator))
                _tasksResponse.value = Response.Success(tasks)
            }
        }
    }

    fun resetPostResponse() {
        _postResponse.value = null
    }

    fun batchDelete(selectedTasks: List<Pair<ITask, IOperatingScope>>, apiContext: ApiContext) {
        viewModelScope.launch {
            val responses = selectedTasks.map { tasksRepository.deleteTask(it.first, it.second, apiContext) }
            _batchDeleteResponse.value = responses
            val tasks = _tasksResponse.value?.valueOrNull()
            if (tasks != null) {
                val currentTasks = tasks.toMutableList()
                responses.forEach { response ->
                    if (response is Response.Success) {
                        currentTasks.remove(response.value)
                    }
                }
                _tasksResponse.value = Response.Success(currentTasks)
            }
        }
    }

    fun resetBatchDeleteResponse() {
        _batchDeleteResponse.value = null
    }

}