package de.deftk.openww.android.filter

import de.deftk.openww.android.R
import de.deftk.openww.api.model.IScope
import java.util.*

class ScopeFilter : Filter<IScope>() {

    val loginCriteria = addCriteria<String>(R.string.email_address, null) { element, value ->
        value ?: return@addCriteria true
        element.login.contains(value, true)
    }

    val nameCriteria = addCriteria<String>(R.string.email_address, null) { element, value ->
        value ?: return@addCriteria true
        element.name.contains(value, true)
    }

}