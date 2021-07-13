package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.filter.NoteFilter
import de.deftk.openww.android.repository.NotesRepository
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.feature.notes.INote
import de.deftk.openww.api.model.feature.notes.NoteColor
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val notesRepository: NotesRepository) : ViewModel() {

    private val _notesResponse = MutableLiveData<Response<List<INote>>>()
    val allNotesResponse: LiveData<Response<List<INote>>> = _notesResponse

    val filter = MutableLiveData(NoteFilter())
    val filteredNotesResponse: LiveData<Response<List<INote>>>
        get() = filter.switchMap { filter ->
            when (filter) {
                null -> allNotesResponse
                else -> allNotesResponse.switchMap { response ->
                    val filtered = MutableLiveData<Response<List<INote>>>()
                    filtered.value = response.smartMap { filter.apply(it) }
                    filtered
                }
            }
        }

    private val _editResponse = MutableLiveData<Response<INote>?>()
    val editResponse: LiveData<Response<INote>?> = _editResponse

    private val _deleteResponse = MutableLiveData<Response<INote>?>()
    val deleteResponse: LiveData<Response<INote>?> = _deleteResponse

    private val _batchDeleteResponse = MutableLiveData<List<Response<INote>>?>()
    val batchDeleteResponse: LiveData<List<Response<INote>>?> = _batchDeleteResponse

    fun loadNotes(apiContext: ApiContext) {
        viewModelScope.launch {
            _notesResponse.value = notesRepository.getNotes(apiContext)
        }
    }

    fun addNote(title: String, text: String, color: NoteColor?, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = notesRepository.addNote(text, title, color, apiContext)
            _editResponse.value = response
            val notesResponse = allNotesResponse.value
            if (response is Response.Success && notesResponse is Response.Success) {
                val notes = notesResponse.value.toMutableList()
                notes.add(response.value)
                _notesResponse.value = Response.Success(notes)
            }

        }
    }

    fun editNote(note: INote, title: String, text: String, color: NoteColor, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = notesRepository.editNote(note, text, title, color, apiContext)
            _editResponse.value = response
            val notesResponse = allNotesResponse.value
            if (response is Response.Success && notesResponse is Response.Success) {
                val notes = notesResponse.value.toMutableList()
                notes[notes.indexOfFirst { it.id == note.id }] = response.value
                _notesResponse.value = Response.Success(notes)
            }
        }
    }

    fun deleteNote(note: INote, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = notesRepository.deleteNote(note, apiContext)
            _deleteResponse.value = response
            val notesResponse = allNotesResponse.value
            if (response is Response.Success && notesResponse is Response.Success) {
                val notes = notesResponse.value.toMutableList()
                notes.remove(note)
                _notesResponse.value = Response.Success(notes)
            }
        }
    }

    fun resetEditResponse() {
        _editResponse.value = null
    }

    fun resetDeleteResponse() {
        _deleteResponse.value = null
    }

    fun batchDelete(selectedTasks: List<INote>, apiContext: ApiContext) {
        viewModelScope.launch {
            val responses = selectedTasks.map { notesRepository.deleteNote(it, apiContext) }
            _batchDeleteResponse.value = responses
            val tasks = allNotesResponse.value?.valueOrNull()
            if (tasks != null) {
                val currentTasks = tasks.toMutableList()
                responses.forEach { response ->
                    if (response is Response.Success) {
                        currentTasks.remove(response.value)
                    }
                }
                _notesResponse.value = Response.Success(currentTasks)
            }
        }
    }

    fun resetBatchDeleteResponse() {
        _batchDeleteResponse.value = null
    }


}