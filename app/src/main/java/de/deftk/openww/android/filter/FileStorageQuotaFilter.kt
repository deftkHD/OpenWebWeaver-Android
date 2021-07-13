package de.deftk.openww.android.filter

import de.deftk.openww.android.R
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.Quota

class FileStorageQuotaFilter : Filter<Pair<IOperatingScope, Quota>>(QuotaOrder.ByOperatorDefault) {

    private val groupDelegate = ScopeFilter()

    val smartSearchCriteria = addCriteria<String>(R.string.smart_search, null) { element, value ->
        value ?: return@addCriteria true
        groupDelegate.nameCriteria.matches(element.first, value)
    }

}