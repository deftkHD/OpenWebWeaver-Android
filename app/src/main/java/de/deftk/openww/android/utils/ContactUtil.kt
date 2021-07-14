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

        if (contact.nickName != null)
            details.add(ContactDetail(contact.nickName!!, NICK_NAME))
        if (contact.fullName != null)
            details.add(ContactDetail(contact.fullName!!, FULL_NAME))
        if (contact.firstName != null)
            details.add(ContactDetail(contact.firstName!!, FIRST_NAME))
        if (contact.middleName != null)
            details.add(ContactDetail(contact.middleName!!, MIDDLE_NAME))
        if (contact.lastName != null)
            details.add(ContactDetail(contact.lastName!!, LAST_NAME))
        if (contact.birthday != null)
            details.add(ContactDetail(contact.birthday!!, BIRTHDAY))
        if (contact.emailAddress != null)
            details.add(ContactDetail(contact.emailAddress!!, EMAIL_ADDRESS))
        if (contact.email2Address != null)
            details.add(ContactDetail(contact.email2Address!!, EMAIL_ADDRESS_2))
        if (contact.email3Address != null)
            details.add(ContactDetail(contact.email3Address!!, EMAIL_ADDRESS_3))
        if (contact.mobilePhone != null)
            details.add(ContactDetail(contact.mobilePhone!!, MOBILE_PHONE))
        if (contact.gender != null)
            details.add(ContactDetail(contact.gender!!, GENDER))
        if (contact.hobby != null)
            details.add(ContactDetail(contact.hobby!!, HOBBIES))
        if (contact.homeCity != null)
            details.add(ContactDetail(contact.homeCity!!, HOME_CITY))
        if (contact.homeCoords != null)
            details.add(ContactDetail(contact.homeCoords!!, HOME_COORDS))
        if (contact.homeCountry != null)
            details.add(ContactDetail(contact.homeCountry!!, HOME_COUNTRY))
        if (contact.homeFax != null)
            details.add(ContactDetail(contact.homeFax!!, HOME_FAX))
        if (contact.homePhone != null)
            details.add(ContactDetail(contact.homePhone!!, HOME_PHONE))
        if (contact.homePostalCode != null)
            details.add(ContactDetail(contact.homePostalCode!!, HOME_POSTAL_CODE))
        if (contact.homeState != null)
            details.add(ContactDetail(contact.homeState!!, HOME_STATE))
        if (contact.homeStreet != null)
            details.add(ContactDetail(contact.homeStreet!!, HOME_STREET))
        if (contact.homeStreet2 != null)
            details.add(ContactDetail(contact.homeStreet2!!, HOME_STREET_2))
        if (contact.homeStreet3 != null)
            details.add(ContactDetail(contact.homeStreet3!!, HOME_STREET_3))
        if (contact.title != null)
            details.add(ContactDetail(contact.title!!, TITLE))
        if (contact.jobTitle != null)
            details.add(ContactDetail(contact.jobTitle!!, JOB_TITLE))
        if (contact.jobTitle2 != null)
            details.add(ContactDetail(contact.jobTitle2!!, JOB_TITLE_2))
        if (contact.suffix != null)
            details.add(ContactDetail(contact.suffix!!, SUFFIX))
        if (contact.company != null)
            details.add(ContactDetail(contact.company!!, COMPANY))
        if (contact.companyType != null)
            details.add(ContactDetail(contact.companyType!!, COMPANY_TYPE))
        if (contact.businessCity != null)
            details.add(ContactDetail(contact.businessCity!!, BUSINESS_CITY))
        if (contact.businessCoords != null)
            details.add(ContactDetail(contact.businessCoords!!, BUSINESS_COORDS))
        if (contact.businessCountry != null)
            details.add(ContactDetail(contact.businessCountry!!, BUSINESS_COUNTRY))
        if (contact.businessFax != null)
            details.add(ContactDetail(contact.businessFax!!, BUSINESS_FAX))
        if (contact.businessPhone != null)
            details.add(ContactDetail(contact.businessPhone!!, BUSINESS_PHONE))
        if (contact.businessPostalCode != null)
            details.add(ContactDetail(contact.businessPostalCode!!, BUSINESS_POSTAL_CODE))
        if (contact.businessState != null)
            details.add(ContactDetail(contact.businessState!!, BUSINESS_STATE))
        if (contact.businessStreet != null)
            details.add(ContactDetail(contact.businessStreet!!, BUSINESS_STREET))
        if (contact.businessStreet2 != null)
            details.add(ContactDetail(contact.businessStreet2!!, BUSINESS_STREET_2))
        if (contact.businessStreet3 != null)
            details.add(ContactDetail(contact.businessStreet3!!, BUSINESS_STREET_3))
        if (contact.subjects != null)
            details.add(ContactDetail(contact.subjects!!, SUBJECTS))
        if (contact.uid != null)
            details.add(ContactDetail(contact.uid!!, UID))
        if (contact.webPage != null)
            details.add(ContactDetail(contact.webPage!!, WEBSITE))
        if (contact.notes != null)
            details.add(ContactDetail(contact.notes!!, NOTES))
        if (contact.categories != null)
            details.add(ContactDetail(contact.categories!!, CATEGORIES))

        return details
    }

    private fun applyDetails(details: List<ContactDetail>, src: IContact): IContact {
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
            src.modified
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

    fun getContactName(contact: IContact): String {
        val name = StringBuilder()

        if (contact.nickName != null) {
            name.append(contact.nickName)
        } else if (contact.fullName != null) {
            name.append(contact.fullName)
        } else {
            if (contact.firstName != null)
                name.append(contact.firstName).append(" ")
            if (contact.middleName != null)
                name.append(contact.middleName).append(" ")
            if (contact.lastName != null)
                name.append(contact.lastName)
        }

        return name.toString().trim()
    }

}