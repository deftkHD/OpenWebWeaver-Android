package de.deftk.openlonet.repository

import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.lonet.api.model.IGroup
import de.deftk.lonet.api.model.IScope
import de.deftk.openlonet.api.Response
import javax.inject.Inject

class GroupRepository @Inject constructor() : AbstractRepository() {

    suspend fun getMembers(group: IGroup, onlineOnly: Boolean, apiContext: ApiContext): Response<List<IScope>> = apiCall {
        group.getMembers(onlineOnly = onlineOnly, context = group.getRequestContext(apiContext)).sortedBy { it.name }
    }

}