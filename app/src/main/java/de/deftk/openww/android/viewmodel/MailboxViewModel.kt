package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.feature.mailbox.IEmail
import de.deftk.openww.api.model.feature.mailbox.IEmailFolder
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.repository.MailboxRepository
import de.deftk.openww.api.model.feature.filestorage.session.ISessionFile
import de.deftk.openww.api.model.feature.mailbox.ReferenceMode
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val mailboxRepository: MailboxRepository) : ViewModel() {

    private val _foldersResponse = MutableLiveData<Response<List<IEmailFolder>>>()
    val foldersResponse: LiveData<Response<List<IEmailFolder>>> = _foldersResponse

    private val _folderPostResponse = MutableLiveData<Response<IEmailFolder?>?>()
    val folderPostResponse: LiveData<Response<IEmailFolder?>?> = _folderPostResponse

    private val _currentFolder = MutableLiveData<IEmailFolder>()
    val currentFolder: LiveData<IEmailFolder> = _currentFolder

    private val _currentMails = MutableLiveData<Response<List<IEmail>>>()
    val currentMails: LiveData<Response<List<IEmail>>> = _currentMails

    private val emailResponses = mutableMapOf<IEmailFolder, MutableLiveData<Response<List<IEmail>>>>()

    private val _emailPostResponse = MutableLiveData<Response<IEmail?>?>()
    val emailPostResponse: LiveData<Response<IEmail?>?> = _emailPostResponse

    private val _emailReadPostResponse = MutableLiveData<Response<IEmail?>?>()
    val emailReadPostResponse: LiveData<Response<IEmail?>?> = _emailReadPostResponse

    private val _emailSendResponse = MutableLiveData<Response<Unit>>()
    val emailSendResponse: LiveData<Response<Unit>> = _emailSendResponse

    fun loadFolders(apiContext: ApiContext) {
        viewModelScope.launch {
            val response = mailboxRepository.getFolders(apiContext)
            _foldersResponse.value = response
            if (response is Response.Success) {
                val currentFolder = currentFolder.value
                if (currentFolder == null || !response.value.contains(currentFolder) || !emailResponses.containsKey(currentFolder)) {
                    selectFolder(response.value.first { it.isInbox }, apiContext)
                }
            }
        }
    }

    fun addFolder(name: String, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = mailboxRepository.addFolder(name, apiContext)
            if (_foldersResponse.value is Response.Success && response is Response.Success) {
                // reload folders because of request not returning any data
                loadFolders(apiContext)
            }
            _folderPostResponse.value = response
        }
    }

    fun selectFolder(folder: IEmailFolder, apiContext: ApiContext) {
        _currentFolder.value = folder
        if (!emailResponses.containsKey(folder)) {
            viewModelScope.launch {
                suspendLoadEmails(folder, apiContext)
                _currentMails.value = emailResponses[folder]?.value ?: Response.Failure(NullPointerException())
            }
        } else {
            _currentMails.value = emailResponses[folder]?.value ?: Response.Failure(NullPointerException())
        }
    }

    private suspend fun suspendLoadEmails(folder: IEmailFolder, apiContext: ApiContext) {
        val response = mailboxRepository.getEmails(folder, apiContext)
        emailResponses.getOrPut(folder) { MutableLiveData() }.value = response
    }

    fun getCachedResponse(folder: IEmailFolder): Response<List<IEmail>>? {
        return emailResponses[folder]?.value
    }

    fun cleanCache() {
        emailResponses.clear()
    }

    fun sendEmail(to: String, subject: String, plainBody: String, cc: String? = null, bcc: String? = null, importSessionFiles: List<ISessionFile>? = null, referenceFolderId: String? = null, referenceMessageId: Int? = null, referenceMode: ReferenceMode? = null, text: String? = null, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = mailboxRepository.sendEmail(to, subject, plainBody, cc, bcc, importSessionFiles, referenceFolderId, referenceMessageId, referenceMode, text, apiContext)
            _emailSendResponse.value = response
        }
    }

    fun readEmail(email: IEmail, folder: IEmailFolder, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = mailboxRepository.readEmail(email, folder, false, apiContext)
            _emailReadPostResponse.value = response
        }
    }

    fun moveEmail(email: IEmail, folder: IEmailFolder, destination: IEmailFolder, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = mailboxRepository.moveEmail(email, folder, destination, apiContext)
            if (response is Response.Success) {
                val storedSrc = getCachedResponse(folder)
                val storedDst = getCachedResponse(destination)
                if (storedSrc is Response.Success) {
                    val newResponse = Response.Success(storedSrc.value.toMutableList().apply {
                        this.remove(email)
                    })
                    emailResponses[folder]!!.value = newResponse
                    if (currentFolder.value == folder) {
                        _currentMails.value = newResponse
                    }
                }
                if (storedDst is Response.Success) {
                    val newResponse = Response.Success(storedDst.value.toMutableList().apply {
                        this.add(0, email)
                    })
                    emailResponses[destination]!!.value = newResponse
                    if (currentFolder.value == destination) {
                        _currentMails.value = newResponse
                    }
                }
            }
            _emailPostResponse.value = response
        }
    }

    fun deleteEmail(email: IEmail, folder: IEmailFolder, allowTrash: Boolean, apiContext: ApiContext) {
        if (allowTrash) {
            val foldersResponse = foldersResponse.value
            if (foldersResponse is Response.Success) {
                val trash = foldersResponse.value.firstOrNull { it.isTrash }
                if (trash != null && folder != trash) {
                    moveEmail(email, folder, trash, apiContext)
                    return
                }
            }
        }
        viewModelScope.launch {
            shredEmail(email, folder, apiContext)
        }
    }

    private suspend fun shredEmail(email: IEmail, folder: IEmailFolder, apiContext: ApiContext) {
        val response = mailboxRepository.deleteEmail(email, folder, apiContext)
        if (response is Response.Success) {
            val stored = getCachedResponse(folder)
            if (stored is Response.Success) {
                val newResponse = Response.Success(stored.value.toMutableList().apply {
                    this.remove(email)
                })
                emailResponses[folder]!!.value = newResponse
                if (currentFolder.value == folder) {
                    _currentMails.value = newResponse
                }
            }
        }
        _emailPostResponse.value = response
    }

    fun resetPostResponse() {
        _emailPostResponse.value = null
    }

    fun resetReadPostResponse() {
        _emailReadPostResponse.value = null
    }

}