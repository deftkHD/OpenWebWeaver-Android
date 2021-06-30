package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.tasks.ITask
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.repository.TasksRepository
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val tasksRepository: TasksRepository): ViewModel() {

    private val _tasksResponse = MutableLiveData<Response<List<Pair<ITask, IOperatingScope>>>>()
    val tasksResponse: LiveData<Response<List<Pair<ITask, IOperatingScope>>>> = _tasksResponse

    private val _postResponse = MutableLiveData<Response<ITask?>?>()
    val postResponse: LiveData<Response<ITask?>?> = _postResponse

    fun loadTasks(apiContext: ApiContext) {
        viewModelScope.launch {
            _tasksResponse.value = tasksRepository.getTasks(apiContext)
        }
    }

    fun addTask(title: String, description: String?, completed: Boolean?, startDate: Date?, dueDate: Date?, scope: IOperatingScope, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = tasksRepository.addTask(title, completed, description, dueDate?.time, startDate?.time, scope, apiContext)
            if (_tasksResponse.value is Response.Success && response is Response.Success) {
                // inject new task into stored livedata
                val tasks = (_tasksResponse.value as Response.Success<List<Pair<ITask, IOperatingScope>>>).value.toMutableList()
                tasks.add(response.value to scope)
                _tasksResponse.value = Response.Success(tasks)
            }
            _postResponse.value = response
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
            if (response is Response.Success) {
                // no need to update items in list because the instance will be the same

                // trigger observers
                _tasksResponse.value = _tasksResponse.value
            }
            _postResponse.value = response.smartMap { task }
        }
    }

    fun deleteTask(task: ITask, operator: IOperatingScope, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = tasksRepository.deleteTask(task, operator, apiContext)
            if (response is Response.Success && _tasksResponse.value is Response.Success) {
                val notifications = (_tasksResponse.value as Response.Success<List<Pair<ITask, IOperatingScope>>>).value.toMutableList()
                notifications.remove(Pair(task, operator))
                _tasksResponse.value = Response.Success(notifications)
            }
            _postResponse.value = Response.Success(task)
        }
    }

    fun resetPostResponse() {
        _postResponse.value = null
    }

}