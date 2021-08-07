package de.deftk.openww.android.filter

import de.deftk.openww.android.R
import de.deftk.openww.android.feature.filestorage.FileCacheElement
import de.deftk.openww.api.model.IOperatingScope

class FileStorageFileFilter : Filter<Pair<FileCacheElement, IOperatingScope>>(FileOrder.Default) {

    private val groupDelegate = ScopeFilter()
    private val creatorDelegate = ScopeFilter()

    val nameCriteria = addCriteria<String>(R.string.name, null) { element, value ->
        value ?: return@addCriteria true
        element.first.file.name.contains(value, true)
    }

    val descriptionCriteria = addCriteria<String>(R.string.description, null) { element, value ->
        value ?: return@addCriteria true
        element.first.file.description?.contains(value, true) == true
    }

    val parentCriteria = addCriteria<String>(0, null) { element, value ->
        value ?: return@addCriteria true
        element.first.file.parentId == value
    }

    val smartSearchCriteria = addCriteria<String>(R.string.smart_search, null) { element, value ->
        value ?: return@addCriteria true
        nameCriteria.matches(element, value)
                || descriptionCriteria.matches(element, value)
                || groupDelegate.nameCriteria.matches(element.second, value)
                || creatorDelegate.loginCriteria.matches(element.first.file.created.member, value)
                || creatorDelegate.nameCriteria.matches(element.first.file.created.member, value)
    }

}