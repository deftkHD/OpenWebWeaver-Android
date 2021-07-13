package de.deftk.openww.android.filter

import de.deftk.openww.android.R
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.board.IBoardNotification

class BoardNotificationFilter : Filter<Pair<IBoardNotification, IGroup>>(BoardNotificationOrder.ByCreatedDesc) {

    private val groupDelegate = ScopeFilter()
    private val creatorDelegate = ScopeFilter()

    val titleCriteria = addCriteria<String>(R.string.title, null) { element, value ->
        value ?: return@addCriteria true
        element.first.title.contains(value, true)
    }

    val descriptionCriteria = addCriteria<String>(R.string.description, null) { element, value ->
        value ?: return@addCriteria true
        element.first.text.contains(value, true)
    }

    val smartSearchCriteria = addCriteria<String>(R.string.smart_search, null) { element, value ->
        value ?: return@addCriteria true
        titleCriteria.matches(element, value)
                || descriptionCriteria.matches(element, value)
                || groupDelegate.nameCriteria.matches(element.second, value)
                || creatorDelegate.loginCriteria.matches(element.first.created.member, value)
                || creatorDelegate.nameCriteria.matches(element.first.created.member, value)
    }

}