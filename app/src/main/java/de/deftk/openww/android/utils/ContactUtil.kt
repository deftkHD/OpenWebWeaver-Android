package de.deftk.openww.android.utils

import de.deftk.openww.android.feature.contacts.ContactDetail
import de.deftk.openww.android.feature.contacts.ContactDetailType
import de.deftk.openww.android.feature.contacts.ContactDetailType.*
import de.deftk.openww.api.implementation.feature.contacts.Contact
import de.deftk.openww.api.model.feature.contacts.Gender
import de.deftk.openww.api.model.feature.contacts.IContact

object ContactUtil {

    fun extractContactDetails(contact: IContact): List<ContactDetail> {
        val details = mutableListOf<ContactDetail>()

        if (contact.getNickName() != null)
            details.add(ContactDetail(contact.getNickName()!!, NICK_NAME))
        if (contact.getFullName() != null)
            details.add(ContactDetail(contact.getFullName()!!, FULL_NAME))
        if (contact.getFirstName() != null)
            details.add(ContactDetail(contact.getFirstName()!!, FIRST_NAME))
        if (contact.getMiddleName() != null)
            details.add(ContactDetail(contact.getMiddleName()!!, MIDDLE_NAME))
        if (contact.getLastName() != null)
            details.add(ContactDetail(contact.getLastName()!!, LAST_NAME))
        if (contact.getBirthday() != null)
            details.add(ContactDetail(contact.getBirthday()!!, BIRTHDAY))
        if (contact.getEmailAddress() != null)
            details.add(ContactDetail(contact.getEmailAddress()!!, EMAIL_ADDRESS))
        if (contact.getEmailAddress2() != null)
            details.add(ContactDetail(contact.getEmailAddress2()!!, EMAIL_ADDRESS_2))
        if (contact.getEmailAddress3() != null)
            details.add(ContactDetail(contact.getEmailAddress3()!!, EMAIL_ADDRESS_3))
        if (contact.getMobilePhone() != null)
            details.add(ContactDetail(contact.getMobilePhone()!!, MOBILE_PHONE))
        if (contact.getGender() != null)
            details.add(ContactDetail(contact.getGender()!!, GENDER))
        if (contact.getHobby() != null)
            details.add(ContactDetail(contact.getHobby()!!, HOBBIES))
        if (contact.getHomeCity() != null)
            details.add(ContactDetail(contact.getHomeCity()!!, HOME_CITY))
        if (contact.getHomeCoords() != null)
            details.add(ContactDetail(contact.getHomeCoords()!!, HOME_COORDS))
        if (contact.getHomeCountry() != null)
            details.add(ContactDetail(contact.getHomeCountry()!!, HOME_COUNTRY))
        if (contact.getHomeFax() != null)
            details.add(ContactDetail(contact.getHomeFax()!!, HOME_FAX))
        if (contact.getHomePhone() != null)
            details.add(ContactDetail(contact.getHomePhone()!!, HOME_PHONE))
        if (contact.getHomePostalCode() != null)
            details.add(ContactDetail(contact.getHomePostalCode()!!, HOME_POSTAL_CODE))
        if (contact.getHomeState() != null)
            details.add(ContactDetail(contact.getHomeState()!!, HOME_STATE))
        if (contact.getHomeStreet() != null)
            details.add(ContactDetail(contact.getHomeStreet()!!, HOME_STREET))
        if (contact.getHomeStreet2() != null)
            details.add(ContactDetail(contact.getHomeStreet2()!!, HOME_STREET_2))
        if (contact.getHomeStreet3() != null)
            details.add(ContactDetail(contact.getHomeStreet3()!!, HOME_STREET_3))
        if (contact.getTitle() != null)
            details.add(ContactDetail(contact.getTitle()!!, TITLE))
        if (contact.getJobTitle() != null)
            details.add(ContactDetail(contact.getJobTitle()!!, JOB_TITLE))
        if (contact.getJobTitle2() != null)
            details.add(ContactDetail(contact.getJobTitle2()!!, JOB_TITLE_2))
        if (contact.getSuffix() != null)
            details.add(ContactDetail(contact.getSuffix()!!, SUFFIX))
        if (contact.getCompany() != null)
            details.add(ContactDetail(contact.getCompany()!!, COMPANY))
        if (contact.getCompanyType() != null)
            details.add(ContactDetail(contact.getCompanyType()!!, COMPANY_TYPE))
        if (contact.getBusinessCity() != null)
            details.add(ContactDetail(contact.getBusinessCity()!!, BUSINESS_CITY))
        if (contact.getBusinessCoords() != null)
            details.add(ContactDetail(contact.getBusinessCoords()!!, BUSINESS_COORDS))
        if (contact.getBusinessCountry() != null)
            details.add(ContactDetail(contact.getBusinessCountry()!!, BUSINESS_COUNTRY))
        if (contact.getBusinessFax() != null)
            details.add(ContactDetail(contact.getBusinessFax()!!, BUSINESS_FAX))
        if (contact.getBusinessPhone() != null)
            details.add(ContactDetail(contact.getBusinessPhone()!!, BUSINESS_PHONE))
        if (contact.getBusinessPostalCode() != null)
            details.add(ContactDetail(contact.getBusinessPostalCode()!!, BUSINESS_POSTAL_CODE))
        if (contact.getBusinessState() != null)
            details.add(ContactDetail(contact.getBusinessState()!!, BUSINESS_STATE))
        if (contact.getBusinessStreet() != null)
            details.add(ContactDetail(contact.getBusinessStreet()!!, BUSINESS_STREET))
        if (contact.getBusinessStreet2() != null)
            details.add(ContactDetail(contact.getBusinessStreet2()!!, BUSINESS_STREET_2))
        if (contact.getBusinessStreet3() != null)
            details.add(ContactDetail(contact.getBusinessStreet3()!!, BUSINESS_STREET_3))
        if (contact.getSubjects() != null)
            details.add(ContactDetail(contact.getSubjects()!!, SUBJECTS))
        if (contact.getUid() != null)
            details.add(ContactDetail(contact.getUid()!!, UID))
        if (contact.getWebPage() != null)
            details.add(ContactDetail(contact.getWebPage()!!, WEBSITE))
        if (contact.getNotes() != null)
            details.add(ContactDetail(contact.getNotes()!!, NOTES))
        if (contact.getCategories() != null)
            details.add(ContactDetail(contact.getCategories()!!, CATEGORIES))

        return details
    }

    fun applyDetails(details: List<ContactDetail>, src: IContact): IContact {
        val yeet = details.getDetailValue(GENDER)
        return Contact(
            src.id,
            details.getStringDetailValue(BIRTHDAY),
            details.getStringDetailValue(BUSINESS_CITY),
            details.getStringDetailValue(BUSINESS_COORDS),
            details.getStringDetailValue(BUSINESS_COUNTRY),
            details.getStringDetailValue(BUSINESS_FAX),
            details.getStringDetailValue(BUSINESS_PHONE),
            details.getStringDetailValue(BUSINESS_POSTAL_CODE),
            details.getStringDetailValue(BUSINESS_STATE),
            details.getStringDetailValue(BUSINESS_STREET),
            details.getStringDetailValue(BUSINESS_STREET_2),
            details.getStringDetailValue(BUSINESS_STREET_3),
            details.getStringDetailValue(CATEGORIES),
            details.getStringDetailValue(COMPANY),
            details.getStringDetailValue(COMPANY_TYPE),
            details.getStringDetailValue(EMAIL_ADDRESS_2),
            details.getStringDetailValue(EMAIL_ADDRESS_3),
            details.getStringDetailValue(EMAIL_ADDRESS),
            details.getStringDetailValue(FIRST_NAME),
            details.getStringDetailValue(FULL_NAME),
            details.getDetailValue(GENDER) as? Gender?,
            details.getStringDetailValue(HOBBIES),
            details.getStringDetailValue(HOME_CITY),
            details.getStringDetailValue(HOME_COORDS),
            details.getStringDetailValue(HOME_COUNTRY),
            details.getStringDetailValue(HOME_FAX),
            details.getStringDetailValue(HOME_PHONE),
            details.getStringDetailValue(HOME_POSTAL_CODE),
            details.getStringDetailValue(HOME_STATE),
            details.getStringDetailValue(HOME_STREET),
            details.getStringDetailValue(HOME_STREET_2),
            details.getStringDetailValue(HOME_STREET_3),
            details.getStringDetailValue(JOB_TITLE),
            details.getStringDetailValue(JOB_TITLE_2),
            details.getStringDetailValue(LAST_NAME),
            details.getStringDetailValue(MIDDLE_NAME),
            details.getStringDetailValue(MOBILE_PHONE),
            details.getStringDetailValue(NICK_NAME),
            details.getStringDetailValue(NOTES),
            details.getStringDetailValue(SUBJECTS),
            details.getStringDetailValue(SUFFIX),
            details.getStringDetailValue(TITLE),
            details.getStringDetailValue(UID),
            details.getStringDetailValue(WEBSITE),
            src.created,
            src.getModified()
        )
    }

    fun removeContactDetail(type: ContactDetailType, contact: IContact): IContact {
        val details = extractContactDetails(contact).toMutableList()
        details.removeAll { it.type == type }
        return applyDetails(details, contact)
    }

    fun editContactDetail(type: ContactDetailType, newValue: Any, contact: IContact): IContact {
        val details = extractContactDetails(contact).toMutableList()
        val target = details.firstOrNull { it.type == type }
        if (target == null) {
            details.add(ContactDetail(newValue, type))
        } else {
            details[details.indexOf(target)] = ContactDetail(newValue, type)
        }
        return applyDetails(details, contact)
    }

    private fun List<ContactDetail>.getDetailValue(type: ContactDetailType): Any? {
        return firstOrNull { it.type == type }?.value
    }

    private fun List<ContactDetail>.getStringDetailValue(type: ContactDetailType): String? {
        return getDetailValue(type)?.toString()
    }

}