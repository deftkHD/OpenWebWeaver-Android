package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.filter.NoteFilter
import de.deftk.openww.android.repository.NotesRepository
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.feature.notes.INote
import de.deftk.openww.api.model.feature.notes.NoteColor
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val notesRepository: NotesRepository) : ScopedViewModel(savedStateHandle) {

    private val _notesResponse = registerProperty<Response<List<INote>>?>("notesResponse", true)
    val allNotesResponse: LiveData<Response<List<INote>>?> = _notesResponse

    val filter = registerProperty("filter", true, NoteFilter())
    val filteredNotesResponse: LiveData<Response<List<INote>>?>
        get() = filter.switchMap { filter ->
            when (filter) {
                null -> allNotesResponse
                else -> allNotesResponse.switchMap { response ->
                    val filtered = registerProperty<Response<List<INote>>?>("filtered", true)
                    filtered.value = response?.smartMap { filter.apply(it) }
                    filtered
                }
            }
        }

    private val _editResponse = registerProperty<Response<INote>?>("editResponse", true)
    val editResponse: LiveData<Response<INote>?> = _editResponse

    private val _deleteResponse = registerProperty<Response<INote>?>("deleteResponse", true)
    val deleteResponse: LiveData<Response<INote>?> = _deleteResponse

    private val _batchDeleteResponse = registerProperty<List<Response<INote>>?>("batchDeleteResponse", true)
    val batchDeleteResponse: LiveData<List<Response<INote>>?> = _batchDeleteResponse

    fun loadNotes(apiContext: IApiContext) {
        viewModelScope.launch {
            _notesResponse.value = notesRepository.getNotes(apiContext)
        }
    }

    fun addNote(title: String, text: String, color: NoteColor?, apiContext: IApiContext) {
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

    fun editNote(note: INote, title: String, text: String, color: NoteColor, apiContext: IApiContext) {
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

    fun deleteNote(note: INote, apiContext: IApiContext) {
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

    fun batchDelete(selectedTasks: List<INote>, apiContext: IApiContext) {
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