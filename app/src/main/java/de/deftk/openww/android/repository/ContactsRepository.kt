package de.deftk.openww.android.repository

import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.contacts.IContact
import javax.inject.Inject

class ContactsRepository @Inject constructor() : AbstractRepository() {

    suspend fun getContacts(scope: IOperatingScope, apiContext: IApiContext) = apiCall {
        scope.getContacts(scope.getRequestContext(apiContext))
    }

    suspend fun addContact(contact: IContact, scope: IOperatingScope, apiContext: IApiContext) = apiCall {
        scope.addContact(contact, scope.getRequestContext(apiContext))
    }

    suspend fun editContact(contact: IContact, scope: IOperatingScope, apiContext: IApiContext) = apiCall {
        contact.edit(
            contact.birthday,
            contact.businessCity,
            contact.businessCoords,
            contact.businessCountry,
            contact.businessFax,
            contact.businessPhone,
            contact.businessPostalCode,
            contact.businessState,
            contact.businessStreet,
            contact.businessStreet2,
            contact.businessStreet3,
            contact.categories,
            contact.company,
            contact.companyType,
            contact.email2Address,
            contact.email3Address,
            contact.emailAddress,
            contact.firstName,
            contact.fullName,
            contact.gender,
            contact.hobby,
            contact.homeCity,
            contact.homeCoords,
            contact.homeCountry,
            contact.homeFax,
            contact.homePhone,
            contact.homePostalCode,
            contact.homeState,
            contact.homeStreet,
            contact.homeStreet2,
            contact.homeStreet3,
            contact.jobTitle,
            contact.jobTitle2,
            contact.lastName,
            contact.middleName,
            contact.mobilePhone,
            contact.nickName,
            contact.notes,
            contact.subjects,
            contact.suffix,
            contact.title,
            contact.uid,
            contact.webPage,
            scope.getRequestContext(apiContext)
        )
        contact
    }

    suspend fun deleteContact(contact: IContact, scope: IOperatingScope, apiContext: IApiContext) = apiCall {
        contact.delete(scope.getRequestContext(apiContext))
        contact to scope
    }

}