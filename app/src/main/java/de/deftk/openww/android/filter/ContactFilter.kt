package de.deftk.openww.android.filter

import de.deftk.openww.android.R
import de.deftk.openww.android.utils.ContactUtil
import de.deftk.openww.api.model.feature.contacts.IContact

class ContactFilter : Filter<IContact>() {

    val nameCriteria = addCriteria<String>(R.string.name, null) { element, value ->
        value ?: return@addCriteria true
        ContactUtil.getContactName(element).contains(value, true)
    }

    val smartSearchCriteria = addCriteria<String>(R.string.smart_search, null) { element, value ->
        value ?: return@addCriteria true
        val fields = ContactUtil.extractContactDetails(element)
        fields.any { it.value.toString().contains(value, true) }
    }

}