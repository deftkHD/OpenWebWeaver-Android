package de.deftk.openww.android.repository

import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.contacts.IContact
import javax.inject.Inject

class ContactsRepository @Inject constructor() : AbstractRepository() {

    suspend fun getContacts(scope: IOperatingScope, apiContext: ApiContext) = apiCall {
        scope.getContacts(scope.getRequestContext(apiContext))
    }

    suspend fun addContact(contact: IContact, scope: IOperatingScope, apiContext: ApiContext) = apiCall {
        scope.addContact(contact, scope.getRequestContext(apiContext))
    }

    suspend fun editContact(contact: IContact, scope: IOperatingScope, apiContext: ApiContext) = apiCall {
        contact.edit(
            contact.getBirthday(),
            contact.getBusinessCity(),
            contact.getBusinessCoords(),
            contact.getBusinessCountry(),
            contact.getBusinessFax(),
            contact.getBusinessPhone(),
            contact.getBusinessPostalCode(),
            contact.getBusinessState(),
            contact.getBusinessStreet(),
            contact.getBusinessStreet2(),
            contact.getBusinessStreet3(),
            contact.getCategories(),
            contact.getCompany(),
            contact.getCompanyType(),
            contact.getEmailAddress2(),
            contact.getEmailAddress3(),
            contact.getEmailAddress3(),
            contact.getFirstName(),
            contact.getFullName(),
            contact.getGender(),
            contact.getHobby(),
            contact.getHomeCity(),
            contact.getHomeCoords(),
            contact.getHomeCountry(),
            contact.getHomeFax(),
            contact.getHomePhone(),
            contact.getHomePostalCode(),
            contact.getHomeState(),
            contact.getHomeStreet(),
            contact.getHomeStreet2(),
            contact.getHomeStreet3(),
            contact.getJobTitle(),
            contact.getJobTitle2(),
            contact.getLastName(),
            contact.getMiddleName(),
            contact.getMobilePhone(),
            contact.getNickName(),
            contact.getNotes(),
            contact.getSubjects(),
            contact.getSuffix(),
            contact.getTitle(),
            contact.getUid(),
            contact.getWebPage(),
            scope.getRequestContext(apiContext)
        )
        contact
    }

    suspend fun deleteContact(contact: IContact, scope: IOperatingScope, apiContext: ApiContext) = apiCall {
        contact.delete(scope.getRequestContext(apiContext))
    }

}