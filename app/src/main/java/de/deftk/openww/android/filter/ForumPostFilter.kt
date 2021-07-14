package de.deftk.openww.android.filter

import de.deftk.openww.android.R
import de.deftk.openww.api.model.feature.forum.IForumPost

class ForumPostFilter : Filter<IForumPost>(ForumPostOrder.ByDateDesc) {

    private val creatorDelegate = ScopeFilter()

    val titleCriteria = addCriteria<String>(R.string.title, null) { element, value ->
        value ?: return@addCriteria true
        element.title.contains(value, true)
    }

    val textCriteria = addCriteria<String>(R.string.text, null) { element, value ->
        value ?: return@addCriteria true
        element.text.contains(value, true)
    }

    val smartSearchCriteria = addCriteria<String>(R.string.smart_search, null) { element, value ->
        value ?: return@addCriteria true
        titleCriteria.matches(element, value)
                || textCriteria.matches(element, value)
                || creatorDelegate.smartSearchCriteria.matches(element.created.member, value)
    }

}