package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.feature.filestorage.DownloadSaveWorker
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
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val mailboxRepository: MailboxRepository) : ScopedViewModel(savedStateHandle) {

    private val _foldersResponse = registerProperty<Response<List<IEmailFolder>>?>("foldersResponse", true)
    val foldersResponse: LiveData<Response<List<IEmailFolder>>?> = _foldersResponse

    private val _folderPostResponse = registerProperty<Response<IEmailFolder?>?>("folderPostResponse", true)
    val folderPostResponse: LiveData<Response<IEmailFolder?>?> = _folderPostResponse

    private val _currentFolder = registerProperty<IEmailFolder?>("currentFolder", true)
    val currentFolder: LiveData<IEmailFolder?> = _currentFolder

    private val _currentMails = registerProperty<Response<List<IEmail>>?>("currentMails", true)
    val allCurrentMails: LiveData<Response<List<IEmail>>?> = _currentMails

    private val _exportSessionFileResponse = registerProperty<Response<FileDownloadUrl>?>("exportSessionFileResponse", true)
    val exportSessionFileResponse: LiveData<Response<FileDownloadUrl>?> = _exportSessionFileResponse

    private val _downloadSaveAttachmentWorkerId = registerProperty<UUID?>("downloadSaveAttachmentWorkerId", true)
    val downloadSaveAttachmentWorkerId: LiveData<UUID?> = _downloadSaveAttachmentWorkerId


    private val emailResponses = mutableMapOf<IEmailFolder, MutableLiveData<Response<List<IEmail>>>>()

    val mailFilter = registerProperty("mailFilter", true, MailFilter())
    val currentFilteredMails: LiveData<Response<List<IEmail>>?>
        get() = mailFilter.switchMap { filter ->
            when (filter) {
                null -> allCurrentMails
                else -> allCurrentMails.switchMap { response ->
                    val filtered = registerProperty<Response<List<IEmail>>?>("filtered", true)
                    filtered.postValue(response?.smartMap { filter.apply(it) })
                    filtered
                }
            }
        }

    private val _emailPostResponse = registerProperty<Response<IEmail?>?>("emailPostResponse", true)
    val emailPostResponse: LiveData<Response<IEmail?>?> = _emailPostResponse

    private val _emailReadPostResponse = registerProperty<Response<IEmail?>?>("emailReadPostResponse", true)
    val emailReadPostResponse: LiveData<Response<IEmail?>?> = _emailReadPostResponse

    private val _emailSendResponse = registerProperty<Response<Unit>?>("emailSendResponse", true)
    val emailSendResponse: LiveData<Response<Unit>?> = _emailSendResponse

    private val _batchMoveResponse = registerProperty<List<Response<IEmail>>?>("batchMoveResponse", true)
    val batchMoveResponse: LiveData<List<Response<IEmail>>?> = _batchMoveResponse

    private val _batchDeleteResponse = registerProperty<List<Response<IEmail>>?>("batchDeleteResponse", true)
    val batchDeleteResponse: LiveData<List<Response<IEmail>>?> = _batchDeleteResponse

    private val _batchEmailSetResponse = registerProperty<List<Response<IEmail>>?>("batchEmailSetResponse", true)
    val batchEmailSetResponse: LiveData<List<Response<IEmail>>?> = _batchEmailSetResponse

    fun loadFolders(apiContext: IApiContext) {
        viewModelScope.launch {
            val response = mailboxRepository.getFolders(apiContext)
            _foldersResponse.postValue(response)
            if (response is Response.Success) {
                val currentFolder = currentFolder.value
                if (currentFolder == null || !response.value.any { it.id == currentFolder.id }) {
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
            _folderPostResponse.postValue(response)
            if (_foldersResponse.value is Response.Success && response is Response.Success) {
                // reload folders because of request not returning any data
                loadFolders(apiContext)
            }
        }
    }

    fun selectFolder(folder: IEmailFolder, apiContext: IApiContext) {
        _currentFolder.postValue(folder)
        if (!emailResponses.containsKey(folder)) {
            viewModelScope.launch {
                suspendLoadEmails(folder, apiContext)
                _currentMails.postValue(emailResponses[folder]?.value ?: Response.Failure(NullPointerException()))
            }
        } else {
            _currentMails.postValue(emailResponses[folder]?.value ?: Response.Failure(NullPointerException()))
        }
    }

    private suspend fun suspendLoadEmails(folder: IEmailFolder, apiContext: IApiContext) {
        val response = mailboxRepository.getEmails(folder, apiContext)
        emailResponses.getOrPut(folder) { registerProperty("emailResponse", true) }.value = response
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
            _emailSendResponse.postValue(response)

            if (response is Response.Success) {
                cleanCache() // needs to refresh folder
                if (currentFolder.value != null) {
                    selectFolder(currentFolder.value!!, apiContext)
                }
            }
        }
    }

    fun readEmail(email: IEmail, folder: IEmailFolder, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = mailboxRepository.readEmail(email, folder, false, apiContext)
            _emailReadPostResponse.postValue(response)

            if (response is Response.Success) {
                val newResponse = emailResponses[folder]?.value?.smartMap {  elements ->
                    val list = elements.toMutableList()
                    list[elements.indexOfFirst { it.id == email.id }] = response.value
                    list
                }
                emailResponses[folder]?.postValue(newResponse)
                if (currentFolder.value == folder) {
                    _currentMails.postValue(newResponse)
                }
            }
        }
    }

    fun moveEmail(email: IEmail, folder: IEmailFolder, destination: IEmailFolder, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = mailboxRepository.moveEmail(email, folder, destination, apiContext)
            _emailPostResponse.postValue(response)
            if (response is Response.Success) {
                val storedSrc = getCachedResponse(folder)
                val storedDst = getCachedResponse(destination)
                if (storedSrc is Response.Success) {
                    val newResponse = Response.Success(storedSrc.value.toMutableList().apply {
                        this.remove(email)
                    })
                    emailResponses[folder]!!.postValue(newResponse)
                    if (currentFolder.value == folder) {
                        _currentMails.postValue(newResponse)
                    }
                }
                if (storedDst is Response.Success) {
                    val newResponse = Response.Success(storedDst.value.toMutableList().apply {
                        this.add(0, email)
                    })
                    emailResponses[destination]!!.postValue(newResponse)
                    if (currentFolder.value == destination) {
                        _currentMails.postValue(newResponse)
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
            _exportSessionFileResponse.postValue(response)
        }
    }

    fun resetPostResponse() {
        _emailPostResponse.postValue(null)
    }

    fun resetReadPostResponse() {
        _emailReadPostResponse.postValue(null)
    }

    fun batchDelete(selectedEmails: List<IEmail>, folder: IEmailFolder, apiContext: IApiContext) {
        val trash = foldersResponse.value?.valueOrNull()?.firstOrNull { it.isTrash }
        if (folder.isTrash || trash == null) {
            viewModelScope.launch {
                val responses = selectedEmails.map { mailboxRepository.deleteEmail(it, folder, apiContext) }
                _batchDeleteResponse.postValue(responses)
                val stored = getCachedResponse(folder)
                if (stored is Response.Success) {
                    val newResponse = Response.Success(stored.value.toMutableList().apply {
                        this.removeAll(responses.mapNotNull { it.valueOrNull() })
                    })
                    emailResponses[folder]!!.postValue(newResponse)
                    if (currentFolder.value == folder) {
                        _currentMails.postValue(newResponse)
                    }
                }
            }
        } else {
            batchMove(selectedEmails, folder, trash, apiContext)
        }
    }

    fun resetBatchDeleteResponse() {
        _batchDeleteResponse.postValue(null)
    }

    fun batchMove(selectedEmails: List<IEmail>, from: IEmailFolder, to: IEmailFolder, apiContext: IApiContext) {
        viewModelScope.launch {
            val responses = selectedEmails.map { mailboxRepository.moveEmail(it, from, to, apiContext) }
            _batchMoveResponse.postValue(responses)
            val storedSrc = getCachedResponse(from)
            val storedDst = getCachedResponse(to)
            if (storedSrc is Response.Success) {
                val newResponse = Response.Success(storedSrc.value.toMutableList().apply {
                    this.removeAll(responses.mapNotNull { it.valueOrNull() })
                })
                emailResponses[from]!!.postValue(newResponse)
                if (currentFolder.value == from) {
                    _currentMails.postValue(newResponse)
                }
            }
            if (storedDst is Response.Success) {
                val newResponse = Response.Success(storedDst.value.toMutableList().apply {
                    this.addAll(0, responses.mapNotNull { it.valueOrNull() })
                })
                emailResponses[to]!!.postValue(newResponse)
                if (currentFolder.value == to) {
                    _currentMails.postValue(newResponse)
                }
            }
        }
    }

    fun resetBatchMoveResponse() {
        _batchMoveResponse.postValue(null)
    }

    fun batchSetEmails(emails: List<IEmail>, folder: IEmailFolder, flagged: Boolean?, unread: Boolean?, apiContext: IApiContext) {
        viewModelScope.launch {
            _batchEmailSetResponse.postValue(emails.map { mailboxRepository.setEmail(it, folder, flagged, unread, apiContext) })
        }
    }

    fun resetDownloadSaveAttachmentWorkerId() {
        _downloadSaveAttachmentWorkerId.postValue(null)
    }

    fun resetEmailSetResponse() {
        _batchEmailSetResponse.postValue(null)
    }

    fun resetExportAttachmentResponse() {
        _exportSessionFileResponse.postValue(null)
    }

    fun startAttachmentSaveDownload(workManager: WorkManager, apiContext: IApiContext, attachment: IAttachment, email: IEmail, folder: IEmailFolder, destinationUrl: String) {
        viewModelScope.launch {
            val response = mailboxRepository.exportAttachment(attachment, email, folder, apiContext)
            if (response is Response.Success) {
                val workRequest = DownloadSaveWorker.createRequest(destinationUrl, response.value.url, attachment)
                _downloadSaveAttachmentWorkerId.postValue(workRequest.id)
                workManager.enqueue(workRequest)
            }
        }
    }
}