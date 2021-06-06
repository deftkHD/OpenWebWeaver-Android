package de.deftk.openlonet.feature.overview

import de.deftk.lonet.api.model.IUser
import de.deftk.lonet.api.request.UserApiRequest
import kotlinx.serialization.json.JsonObject

interface OverviewBuilder {

    fun appendRequests(request: UserApiRequest, user: IUser): List<Int>
    fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement

}