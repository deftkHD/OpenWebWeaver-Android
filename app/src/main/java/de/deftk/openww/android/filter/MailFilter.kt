package de.deftk.openww.android.filter

import de.deftk.openww.android.R
import de.deftk.openww.api.model.feature.mailbox.IEmail

class MailFilter : Filter<IEmail>(MailOrder.ByDateDesc) {

    val subjectFilter = addCriteria<String>(R.string.mail_subject, null) { element, value ->
        value ?: return@addCriteria true
        element.subject.contains(value, true)
    }

    val senderFilter = addCriteria<String>(R.string.from, null) { element, value ->
        value ?: return@addCriteria true
        element.from?.any { it.name.contains(value, true) } == true
    }

    val receiverFilter = addCriteria<String>(R.string.mail_to, null) { element, value ->
        value ?: return@addCriteria true
        element.to?.any { it.name.contains(value, true) } == true || element.cc?.any { it.name.contains(value, true) } == true
    }

    val smartSearchCriteria = addCriteria<String>(R.string.smart_search, null) { element, value ->
        value ?: return@addCriteria true
        subjectFilter.matches(element, value)
                || senderFilter.matches(element, value)
                || receiverFilter.matches(element, value)
    }

}