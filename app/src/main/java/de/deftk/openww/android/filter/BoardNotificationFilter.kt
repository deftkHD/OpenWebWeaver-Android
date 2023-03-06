package de.deftk.openww.android.filter

import de.deftk.openww.android.R
import de.deftk.openww.android.feature.board.BoardNotification

class BoardNotificationFilter : Filter<BoardNotification>(BoardNotificationOrder.ByCreatedDesc) {

    private val groupDelegate = ScopeFilter()
    private val creatorDelegate = ScopeFilter()

    val titleCriteria = addCriteria<String>(R.string.title, null) { element, value ->
        value ?: return@addCriteria true
        element.notification.title.contains(value, true)
    }

    val descriptionCriteria = addCriteria<String>(R.string.description, null) { element, value ->
        value ?: return@addCriteria true
        element.notification.text.contains(value, true)
    }

    val smartSearchCriteria = addCriteria<String>(R.string.smart_search, null) { element, value ->
        value ?: return@addCriteria true
        titleCriteria.matches(element, value)
                || descriptionCriteria.matches(element, value)
                || groupDelegate.nameCriteria.matches(element.group, value)
                || creatorDelegate.loginCriteria.matches(element.notification.created.member, value)
                || creatorDelegate.nameCriteria.matches(element.notification.created.member, value)
    }

}