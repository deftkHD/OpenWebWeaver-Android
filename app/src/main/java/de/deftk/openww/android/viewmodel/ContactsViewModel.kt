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
class ContactsViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val contactsRepository: ContactsRepository) : ScopedViewModel(savedStateHandle) {

    private val _contactsResponses = mutableMapOf<IOperatingScope, MutableLiveData<Response<List<IContact>>?>>()

    val filter = registerProperty("filter", true, ContactFilter())
    private val filteredContactsResponses = mutableMapOf<IOperatingScope, LiveData<Response<List<IContact>>?>>()

    private val _editResponse = registerProperty<Response<IContact>?>("editResponse", true)
    val editResponse: LiveData<Response<IContact>?> = _editResponse

    private val _deleteResponse = registerProperty<Response<Pair<IContact, IOperatingScope>>?>("deleteResponse", true)
    val deleteResponse: LiveData<Response<Pair<IContact, IOperatingScope>>?> = _deleteResponse

    private val _batchDeleteResponse = registerProperty<List<Response<Pair<IContact, IOperatingScope>>>?>("batchDeleteResponse", true)
    val batchDeleteResponse: LiveData<List<Response<Pair<IContact, IOperatingScope>>>?> = _batchDeleteResponse

    fun getAllContactsLiveData(scope: IOperatingScope): LiveData<Response<List<IContact>>?> {
        return _contactsResponses.getOrPut(scope) { registerProperty("contactResponse", true) }
    }

    fun getFilteredContactsLiveData(scope: IOperatingScope): LiveData<Response<List<IContact>>?> {
        return filteredContactsResponses.getOrPut(scope) {
            filter.switchMap { filter ->
                when (filter) {
                    null -> getAllContactsLiveData(scope)
                    else -> getAllContactsLiveData(scope).switchMap { response ->
                        val filtered = registerProperty<Response<List<IContact>>?>("filtered", true)
                        filtered.postValue(response?.smartMap { filter.apply(it) })
                        filtered
                    }
                }
            }
        }
    }

    fun loadContacts(scope: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            _contactsResponses.getOrPut(scope) { registerProperty("contactResponse", true) }.value  = contactsRepository.getContacts(scope, apiContext)
        }
    }

    fun addContact(contact: IContact, scope: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = contactsRepository.addContact(contact, scope, apiContext)
            _editResponse.postValue(response)
            val contactsResponse = getAllContactsLiveData(scope).value
            if (response is Response.Success && contactsResponse is Response.Success) {
                val contacts = contactsResponse.value.toMutableList()
                contacts.add(response.value)
                (getAllContactsLiveData(scope) as MutableLiveData).postValue(Response.Success(contacts))
            }
        }
    }

    fun editContact(contact: IContact, scope: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = contactsRepository.editContact(contact, scope, apiContext)
            _editResponse.postValue(response)
            val contactsResponse = getAllContactsLiveData(scope).value
            if (response is Response.Success && contactsResponse is Response.Success) {
                val contacts = contactsResponse.value.toMutableList()
                contacts[contacts.indexOfFirst { it.id == contact.id }] = contact
                (getAllContactsLiveData(scope) as MutableLiveData).postValue(Response.Success(contacts))
            }
        }
    }

    fun deleteContact(contact: IContact, scope: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = contactsRepository.deleteContact(contact, scope, apiContext)
            _deleteResponse.postValue(response)
            val contactsResponse = getAllContactsLiveData(scope).value
            if (response is Response.Success && contactsResponse is Response.Success) {
                val contacts = contactsResponse.value.toMutableList()
                contacts.remove(contact)
                (getAllContactsLiveData(scope) as MutableLiveData).postValue(Response.Success(contacts))
            }
        }
    }

    fun resetEditResponse() {
        _editResponse.postValue(null)
    }

    fun resetDeleteResponse() {
        _deleteResponse.postValue(null)
    }

    fun batchDelete(contacts: List<Pair<IOperatingScope, IContact>>, apiContext: IApiContext) {
        viewModelScope.launch {
            val responses = contacts.map { contactsRepository.deleteContact(it.second, it.first, apiContext) }
            _batchDeleteResponse.postValue(responses)
            responses.forEach { response ->
                if (response is Response.Success) {
                    val liveData = _contactsResponses[response.value.second]
                    if (liveData?.value is Response.Success) {
                        val currentContacts = (liveData.value!! as Response.Success).value.toMutableList()
                        currentContacts.remove(response.value.first)
                        liveData.postValue(Response.Success(currentContacts))
                    }

                }
            }
        }
    }

    fun resetBatchDeleteResponse() {
        _batchDeleteResponse.postValue(null)
    }

}