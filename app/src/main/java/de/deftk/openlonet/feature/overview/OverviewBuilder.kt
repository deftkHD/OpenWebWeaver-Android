package de.deftk.openlonet.feature.overview

import com.google.gson.JsonObject
import de.deftk.lonet.api.request.UserApiRequest

interface OverviewBuilder {

    fun appendRequests(request: UserApiRequest): List<Int>
    fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement

}