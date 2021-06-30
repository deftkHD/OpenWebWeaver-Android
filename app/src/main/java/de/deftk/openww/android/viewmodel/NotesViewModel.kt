package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.repository.NotesRepository
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.feature.notes.INote
import de.deftk.openww.api.model.feature.notes.NoteColor
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val notesRepository: NotesRepository) : ViewModel() {

    private val _notesResponse = MutableLiveData<Response<List<INote>>>()
    val notesResponse: LiveData<Response<List<INote>>> = _notesResponse

    private val _editResponse = MutableLiveData<Response<INote>?>()
    val editResponse: LiveData<Response<INote>?> = _editResponse

    private val _deleteResponse = MutableLiveData<Response<Unit>?>()
    val deleteResponse: LiveData<Response<Unit>?> = _deleteResponse

    fun loadNotes(apiContext: ApiContext) {
        viewModelScope.launch {
            _notesResponse.value = notesRepository.getNotes(apiContext)
        }
    }

    fun addNote(title: String, text: String, color: NoteColor?, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = notesRepository.addNote(text, title, color, apiContext)
            val notesResponse = notesResponse.value
            if (response is Response.Success && notesResponse is Response.Success) {
                val notes = notesResponse.value.toMutableList()
                notes.add(response.value)
                _notesResponse.value = Response.Success(notes)
            }
            _editResponse.value = response
        }
    }

    fun editNote(note: INote, title: String, text: String, color: NoteColor, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = notesRepository.editNote(note, text, title, color, apiContext)
            val notesResponse = notesResponse.value
            if (response is Response.Success && notesResponse is Response.Success) {
                val notes = notesResponse.value.toMutableList()
                notes[notes.indexOfFirst { it.id == note.id }] = response.value
                _notesResponse.value = Response.Success(notes)
            }
            _editResponse.value = response
        }
    }

    fun deleteNote(note: INote, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = notesRepository.deleteNote(note, apiContext)
            val notesResponse = notesResponse.value
            if (response is Response.Success && notesResponse is Response.Success) {
                val notes = notesResponse.value.toMutableList()
                notes.remove(note)
                _notesResponse.value = Response.Success(notes)
            }
            _deleteResponse.value = response
        }
    }

    fun resetEditResponse() {
        _editResponse.value = null
    }

    fun resetDeleteResponse() {
        _deleteResponse.value = null
    }


}