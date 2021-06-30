package de.deftk.openww.android.repository

import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.feature.notes.INote
import de.deftk.openww.api.model.feature.notes.NoteColor
import javax.inject.Inject

class NotesRepository @Inject constructor() : AbstractRepository() {

    suspend fun getNotes(apiContext: ApiContext) = apiCall {
        apiContext.getUser().getNotes(apiContext.getUser().getRequestContext(apiContext))
    }

    suspend fun addNote(text: String, title: String, color: NoteColor?, apiContext: ApiContext) = apiCall {
        apiContext.getUser().addNote(text, title, color, apiContext.getUser().getRequestContext(apiContext))
    }

    suspend fun editNote(note: INote, text: String, title: String, color: NoteColor, apiContext: ApiContext) = apiCall {
        println("COLOR=$color")
        note.edit(title, text, color, apiContext.getUser().getRequestContext(apiContext))
        note
    }

    suspend fun deleteNote(note: INote, apiContext: ApiContext) = apiCall {
        note.delete(apiContext.getUser().getRequestContext(apiContext))
    }

}