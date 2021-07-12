package de.deftk.openww.android.repository

import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.feature.notes.INote
import de.deftk.openww.api.model.feature.notes.NoteColor
import javax.inject.Inject

class NotesRepository @Inject constructor() : AbstractRepository() {

    suspend fun getNotes(apiContext: ApiContext) = apiCall {
        apiContext.user.getNotes(apiContext.user.getRequestContext(apiContext))
    }

    suspend fun addNote(text: String, title: String, color: NoteColor?, apiContext: ApiContext) = apiCall {
        apiContext.user.addNote(text, title, color, apiContext.user.getRequestContext(apiContext))
    }

    suspend fun editNote(note: INote, text: String, title: String, color: NoteColor, apiContext: ApiContext) = apiCall {
        note.edit(title, text, color, apiContext.user.getRequestContext(apiContext))
        note
    }

    suspend fun deleteNote(note: INote, apiContext: ApiContext) = apiCall {
        note.delete(apiContext.user.getRequestContext(apiContext))
        note
    }

}