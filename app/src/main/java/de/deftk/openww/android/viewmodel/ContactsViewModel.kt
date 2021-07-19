package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.filter.ContactFilter
import de.deftk.openww.android.repository.ContactsRepository
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.contacts.IContact
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val contactsRepository: ContactsRepository) : ViewModel() {

    private val _contactsResponses = mutableMapOf<IOperatingScope, MutableLiveData<Response<List<IContact>>>>()

    val filter = MutableLiveData(ContactFilter())
    private val filteredContactsResponses = mutableMapOf<IOperatingScope, LiveData<Response<List<IContact>>>>()

    private val _editResponse = MutableLiveData<Response<IContact>?>()
    val editResponse: LiveData<Response<IContact>?> = _editResponse

    private val _deleteResponse = MutableLiveData<Response<Pair<IContact, IOperatingScope>>?>()
    val deleteResponse: LiveData<Response<Pair<IContact, IOperatingScope>>?> = _deleteResponse

    private val _batchDeleteResponse = MutableLiveData<List<Response<Pair<IContact, IOperatingScope>>>?>()
    val batchDeleteResponse: LiveData<List<Response<Pair<IContact, IOperatingScope>>>?> = _batchDeleteResponse

    fun getAllContactsLiveData(scope: IOperatingScope): LiveData<Response<List<IContact>>> {
        return _contactsResponses.getOrPut(scope) { MutableLiveData() }
    }

    fun getFilteredContactsLiveData(scope: IOperatingScope): LiveData<Response<List<IContact>>> {
        return filteredContactsResponses.getOrPut(scope) {
            filter.switchMap { filter ->
                when (filter) {
                    null -> getAllContactsLiveData(scope)
                    else -> getAllContactsLiveData(scope).switchMap { response ->
                        val filtered = MutableLiveData<Response<List<IContact>>>()
                        filtered.value = response.smartMap { filter.apply(it) }
                        filtered
                    }
                }
            }
        }
    }

    fun loadContacts(scope: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            _contactsResponses.getOrPut(scope) { MutableLiveData() }.value  = contactsRepository.getContacts(scope, apiContext)
        }
    }

    fun addContact(contact: IContact, scope: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = contactsRepository.addContact(contact, scope, apiContext)
            _editResponse.value = response
            val contactsResponse = getAllContactsLiveData(scope).value
            if (response is Response.Success && contactsResponse is Response.Success) {
                val contacts = contactsResponse.value.toMutableList()
                contacts.add(response.value)
                (getAllContactsLiveData(scope) as MutableLiveData).value = Response.Success(contacts)
            }
        }
    }

    fun editContact(contact: IContact, scope: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = contactsRepository.editContact(contact, scope, apiContext)
            _editResponse.value = response
            val contactsResponse = getAllContactsLiveData(scope).value
            if (response is Response.Success && contactsResponse is Response.Success) {
                val contacts = contactsResponse.value.toMutableList()
                contacts[contacts.indexOfFirst { it.id == contact.id }] = contact
                (getAllContactsLiveData(scope) as MutableLiveData).value = Response.Success(contacts)
            }
        }
    }

    fun deleteContact(contact: IContact, scope: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = contactsRepository.deleteContact(contact, scope, apiContext)
            _deleteResponse.value = response
            val contactsResponse = getAllContactsLiveData(scope).value
            if (response is Response.Success && contactsResponse is Response.Success) {
                val contacts = contactsResponse.value.toMutableList()
                contacts.remove(contact)
                (getAllContactsLiveData(scope) as MutableLiveData).value = Response.Success(contacts)
            }
        }
    }

    fun resetEditResponse() {
        _editResponse.value = null
    }

    fun resetDeleteResponse() {
        _deleteResponse.value = null
    }

    fun batchDelete(contacts: List<Pair<IOperatingScope, IContact>>, apiContext: IApiContext) {
        viewModelScope.launch {
            val responses = contacts.map { contactsRepository.deleteContact(it.second, it.first, apiContext) }
            _batchDeleteResponse.value = responses
            responses.forEach { response ->
                if (response is Response.Success) {
                    val liveData = _contactsResponses[response.value.second]
                    if (liveData?.value is Response.Success) {
                        val currentContacts = (liveData.value!! as Response.Success).value.toMutableList()
                        currentContacts.remove(response.value.first)
                        liveData.value = Response.Success(currentContacts)
                    }

                }
            }
        }
    }

    fun resetBatchDeleteResponse() {
        _batchDeleteResponse.value = null
    }

}