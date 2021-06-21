package de.deftk.openww.android.feature.overview

import de.deftk.openww.api.model.IUser
import de.deftk.openww.api.request.UserApiRequest
import kotlinx.serialization.json.JsonObject

interface OverviewBuilder {

    fun appendRequests(request: UserApiRequest, user: IUser): List<Int>
    fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement

}