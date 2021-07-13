package de.deftk.openww.android.filter

import android.content.Context
import de.deftk.openww.android.R
import de.deftk.openww.android.utils.UIUtil
import de.deftk.openww.api.model.feature.systemnotification.ISystemNotification

class SystemNotificationFilter(private val context: Context) : Filter<ISystemNotification>(SystemNotificationOrder.ByDateDesc) {

    private val groupDelegate = ScopeFilter()
    private val creatorDelegate = ScopeFilter()

    val titleCriteria = addCriteria<String>(R.string.title, null) { element, value ->
        value ?: return@addCriteria true
        context.getString(UIUtil.getTranslatedSystemNotificationTitle(element)).contains(value, true)
    }

    val messageCriteria = addCriteria<String>(R.string.message, null) { element, value ->
        value ?: return@addCriteria true
        element.message.contains(value, true)
    }

    val smartSearchCriteria = addCriteria<String>(R.string.smart_search, null) { element, value ->
        value ?: return@addCriteria true
        titleCriteria.matches(element, value)
                || messageCriteria.matches(element, value)
                || groupDelegate.nameCriteria.matches(element.group, value)
                || creatorDelegate.loginCriteria.matches(element.member, value)
                || creatorDelegate.nameCriteria.matches(element.member, value)
    }

}