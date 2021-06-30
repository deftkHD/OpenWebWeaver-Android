package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.repository.ContactsRepository
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.contacts.IContact
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val contactsRepository: ContactsRepository) : ViewModel() {

    private val _contactsResponses = mutableMapOf<IOperatingScope, MutableLiveData<Response<List<IContact>>>>()

    private val _editResponse = MutableLiveData<Response<IContact>?>()
    val editResponse: LiveData<Response<IContact>?> = _editResponse

    private val _deleteResponse = MutableLiveData<Response<Unit>?>()
    val deleteResponse: LiveData<Response<Unit>?> = _deleteResponse

    fun getContactsLiveData(scope: IOperatingScope): LiveData<Response<List<IContact>>> {
        return _contactsResponses.getOrPut(scope) { MutableLiveData() }
    }

    fun loadContacts(scope: IOperatingScope, apiContext: ApiContext) {
        viewModelScope.launch {
            _contactsResponses.getOrPut(scope) { MutableLiveData() }.value  = contactsRepository.getContacts(scope, apiContext)
        }
    }

    fun addContact(contact: IContact, scope: IOperatingScope, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = contactsRepository.addContact(contact, scope, apiContext)
            val contactsResponse = getContactsLiveData(scope).value
            if (response is Response.Success && contactsResponse is Response.Success) {
                val contacts = contactsResponse.value.toMutableList()
                contacts.add(response.value)
                (getContactsLiveData(scope) as MutableLiveData).value = Response.Success(contacts)
            }
            _editResponse.value = response
        }
    }

    fun editContact(contact: IContact, scope: IOperatingScope, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = contactsRepository.editContact(contact, scope, apiContext)
            val contactsResponse = getContactsLiveData(scope).value
            if (response is Response.Success && contactsResponse is Response.Success) {
                val contacts = contactsResponse.value.toMutableList()
                contacts[contacts.indexOfFirst { it.id == contact.id }] = contact
                (getContactsLiveData(scope) as MutableLiveData).value = Response.Success(contacts)
            }
            _editResponse.value = response
        }
    }

    fun deleteContact(contact: IContact, scope: IOperatingScope, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = contactsRepository.deleteContact(contact, scope, apiContext)
            val contactsResponse = getContactsLiveData(scope).value
            if (response is Response.Success && contactsResponse is Response.Success) {
                val contacts = contactsResponse.value.toMutableList()
                contacts.remove(contact)
                (getContactsLiveData(scope) as MutableLiveData).value = Response.Success(contacts)
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