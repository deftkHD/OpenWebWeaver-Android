package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.filter.MailFilter
import de.deftk.openww.android.repository.MailboxRepository
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.feature.FileDownloadUrl
import de.deftk.openww.api.model.feature.filestorage.session.ISessionFile
import de.deftk.openww.api.model.feature.mailbox.IAttachment
import de.deftk.openww.api.model.feature.mailbox.IEmail
import de.deftk.openww.api.model.feature.mailbox.IEmailFolder
import de.deftk.openww.api.model.feature.mailbox.ReferenceMode
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val mailboxRepository: MailboxRepository) : ScopedViewModel() {

    private val _foldersResponse = MutableLiveData<Response<List<IEmailFolder>>?>()
    val foldersResponse: LiveData<Response<List<IEmailFolder>>?> = _foldersResponse

    private val _folderPostResponse = MutableLiveData<Response<IEmailFolder?>?>()
    val folderPostResponse: LiveData<Response<IEmailFolder?>?> = _folderPostResponse

    private val _currentFolder = MutableLiveData<IEmailFolder?>()
    val currentFolder: LiveData<IEmailFolder?> = _currentFolder

    private val _currentMails = MutableLiveData<Response<List<IEmail>>?>()
    val allCurrentMails: LiveData<Response<List<IEmail>>?> = _currentMails

    private val _exportSessionFileResponse = MutableLiveData<Response<FileDownloadUrl>?>()
    val exportSessionFileResponse: LiveData<Response<FileDownloadUrl>?> = _exportSessionFileResponse

    private val emailResponses = mutableMapOf<IEmailFolder, MutableLiveData<Response<List<IEmail>>>>()

    val mailFilter = MutableLiveData(MailFilter())
    val currentFilteredMails: LiveData<Response<List<IEmail>>?>
        get() = mailFilter.switchMap { filter ->
            when (filter) {
                null -> allCurrentMails
                else -> allCurrentMails.switchMap { response ->
                    val filtered = MutableLiveData<Response<List<IEmail>>?>()
                    filtered.value = response?.smartMap { filter.apply(it) }
                    filtered
                }
            }
        }

    private val _emailPostResponse = MutableLiveData<Response<IEmail?>?>()
    val emailPostResponse: LiveData<Response<IEmail?>?> = _emailPostResponse

    private val _emailReadPostResponse = MutableLiveData<Response<IEmail?>?>()
    val emailReadPostResponse: LiveData<Response<IEmail?>?> = _emailReadPostResponse

    private val _emailSendResponse = MutableLiveData<Response<Unit>?>()
    val emailSendResponse: LiveData<Response<Unit>?> = _emailSendResponse

    private val _batchMoveResponse = MutableLiveData<List<Response<IEmail>>?>()
    val batchMoveResponse: LiveData<List<Response<IEmail>>?> = _batchMoveResponse

    private val _batchDeleteResponse = MutableLiveData<List<Response<IEmail>>?>()
    val batchDeleteResponse: LiveData<List<Response<IEmail>>?> = _batchDeleteResponse

    private val _batchEmailSetResponse = MutableLiveData<List<Response<IEmail>>?>()
    val batchEmailSetResponse: LiveData<List<Response<IEmail>>?> = _batchEmailSetResponse

    fun loadFolders(apiContext: IApiContext) {
        viewModelScope.launch {
            val response = mailboxRepository.getFolders(apiContext)
            _foldersResponse.value = response
            if (response is Response.Success) {
                val currentFolder = currentFolder.value
                if (currentFolder == null || !response.value.contains(currentFolder)) {
                    selectFolder(response.value.first { it.isInbox }, apiContext)
                } else {
                    selectFolder(currentFolder, apiContext)
                }
            }
        }
    }

    fun addFolder(name: String, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = mailboxRepository.addFolder(name, apiContext)
            _folderPostResponse.value = response
            if (_foldersResponse.value is Response.Success && response is Response.Success) {
                // reload folders because of request not returning any data
                loadFolders(apiContext)
            }
        }
    }

    fun selectFolder(folder: IEmailFolder, apiContext: IApiContext) {
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

    private suspend fun suspendLoadEmails(folder: IEmailFolder, apiContext: IApiContext) {
        val response = mailboxRepository.getEmails(folder, apiContext)
        emailResponses.getOrPut(folder) { MutableLiveData() }.value = response
    }

    fun getCachedResponse(folder: IEmailFolder): Response<List<IEmail>>? {
        return emailResponses[folder]?.value
    }

    fun cleanCache() {
        emailResponses.clear()
    }

    fun sendEmail(to: String, subject: String, plainBody: String, cc: String? = null, bcc: String? = null, importSessionFiles: List<ISessionFile>? = null, referenceFolderId: String? = null, referenceMessageId: Int? = null, referenceMode: ReferenceMode? = null, text: String? = null, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = mailboxRepository.sendEmail(to, subject, plainBody, cc, bcc, importSessionFiles, referenceFolderId, referenceMessageId, referenceMode, text, apiContext)
            _emailSendResponse.value = response
        }
    }

    fun readEmail(email: IEmail, folder: IEmailFolder, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = mailboxRepository.readEmail(email, folder, false, apiContext)
            _emailReadPostResponse.value = response
        }
    }

    fun moveEmail(email: IEmail, folder: IEmailFolder, destination: IEmailFolder, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = mailboxRepository.moveEmail(email, folder, destination, apiContext)
            _emailPostResponse.value = response
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
        }
    }

    fun deleteEmail(email: IEmail, folder: IEmailFolder, allowTrash: Boolean, apiContext: IApiContext) {
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

    private suspend fun shredEmail(email: IEmail, folder: IEmailFolder, apiContext: IApiContext) {
        val response = mailboxRepository.deleteEmail(email, folder, apiContext)
        _emailPostResponse.value = response
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
    }

    fun exportAttachment(attachment: IAttachment, email: IEmail, folder: IEmailFolder, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = mailboxRepository.exportAttachment(attachment, email, folder, apiContext)
            _exportSessionFileResponse.value = response
        }
    }

    fun resetPostResponse() {
        _emailPostResponse.value = null
    }

    fun resetReadPostResponse() {
        _emailReadPostResponse.value = null
    }

    fun batchDelete(selectedEmails: List<IEmail>, folder: IEmailFolder, apiContext: IApiContext) {
        val trash = foldersResponse.value?.valueOrNull()?.firstOrNull { it.isTrash }
        if (folder.isTrash || trash == null) {
            viewModelScope.launch {
                val responses = selectedEmails.map { mailboxRepository.deleteEmail(it, folder, apiContext) }
                _batchDeleteResponse.value = responses
                val stored = getCachedResponse(folder)
                if (stored is Response.Success) {
                    val newResponse = Response.Success(stored.value.toMutableList().apply {
                        this.removeAll(responses.mapNotNull { it.valueOrNull() })
                    })
                    emailResponses[folder]!!.value = newResponse
                    if (currentFolder.value == folder) {
                        _currentMails.value = newResponse
                    }
                }
            }
        } else {
            batchMove(selectedEmails, folder, trash, apiContext)
        }
    }

    fun resetBatchDeleteResponse() {
        _batchDeleteResponse.value = null
    }

    fun batchMove(selectedEmails: List<IEmail>, from: IEmailFolder, to: IEmailFolder, apiContext: IApiContext) {
        viewModelScope.launch {
            val responses = selectedEmails.map { mailboxRepository.moveEmail(it, from, to, apiContext) }
            _batchMoveResponse.value = responses
            val storedSrc = getCachedResponse(from)
            val storedDst = getCachedResponse(to)
            if (storedSrc is Response.Success) {
                val newResponse = Response.Success(storedSrc.value.toMutableList().apply {
                    this.removeAll(responses.mapNotNull { it.valueOrNull() })
                })
                emailResponses[from]!!.value = newResponse
                if (currentFolder.value == from) {
                    _currentMails.value = newResponse
                }
            }
            if (storedDst is Response.Success) {
                val newResponse = Response.Success(storedDst.value.toMutableList().apply {
                    this.addAll(0, responses.mapNotNull { it.valueOrNull() })
                })
                emailResponses[to]!!.value = newResponse
                if (currentFolder.value == to) {
                    _currentMails.value = newResponse
                }
            }
        }
    }

    fun resetBatchMoveResponse() {
        _batchMoveResponse.value = null
    }

    fun batchSetEmails(emails: List<IEmail>, folder: IEmailFolder, flagged: Boolean?, unread: Boolean?, apiContext: IApiContext) {
        viewModelScope.launch {
            _batchEmailSetResponse.value = emails.map { mailboxRepository.setEmail(it, folder, flagged, unread, apiContext) }
        }
    }

    fun resetEmailSetResponse() {
        _batchEmailSetResponse.value = null
    }

    override fun resetScopedData() {
        _foldersResponse.value = null
        _folderPostResponse.value = null
        _currentFolder.value = null
        _currentMails.value = null
        _emailPostResponse.value = null
        _emailReadPostResponse.value = null
        _emailSendResponse.value = null
        _batchMoveResponse.value = null
        _batchDeleteResponse.value = null
        _batchEmailSetResponse.value = null
        mailFilter.value = MailFilter()
    }

    fun resetExportAttachmentResponse() {
        _exportSessionFileResponse.value = null
    }
}