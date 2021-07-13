package de.deftk.openww.android.filter

import de.deftk.openww.android.R
import de.deftk.openww.api.model.feature.messenger.IQuickMessage

class MessageFilter : Filter<IQuickMessage>(MessageOrder.ByDateCreatedAsc) {

    private val user1ScopeDelegate = ScopeFilter()
    private val user2ScopeDelegate = ScopeFilter()

    val messageCriteria = addCriteria<String>(R.string.message, null) { element, value ->
        value ?: return@addCriteria true
        element.text?.contains(value, true) == true
    }

    val fileNameCriteria = addCriteria<String>(R.string.attachment, null) { element, value ->
        value ?: return@addCriteria true
        element.fileName?.contains(value, true) == true
    }

    val smartSearchCriteria = addCriteria<String>(R.string.smart_search, null) { element, value ->
        value ?: return@addCriteria true
        messageCriteria.matches(element, value)
                || fileNameCriteria.matches(element, value)
                || user1ScopeDelegate.smartSearchCriteria.matches(element.from, value)
                || user2ScopeDelegate.smartSearchCriteria.matches(element.from, value)
    }


}