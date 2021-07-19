package de.deftk.openww.android.repository

import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.IScope
import de.deftk.openww.android.api.Response
import de.deftk.openww.api.model.IApiContext
import javax.inject.Inject

class GroupRepository @Inject constructor() : AbstractRepository() {

    suspend fun getMembers(group: IGroup, onlineOnly: Boolean, apiContext: IApiContext): Response<List<IScope>> = apiCall {
        group.getMembers(onlineOnly = onlineOnly, context = group.getRequestContext(apiContext)).sortedBy { it.name }
    }

}