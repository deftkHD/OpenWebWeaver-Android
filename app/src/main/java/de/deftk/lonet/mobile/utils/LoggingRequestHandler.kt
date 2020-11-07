package de.deftk.lonet.mobile.utils

import android.util.Log
import de.deftk.lonet.api.LoNet
import de.deftk.lonet.api.model.abstract.IContext
import de.deftk.lonet.api.request.ApiRequest
import de.deftk.lonet.api.request.handler.IRequestHandler
import de.deftk.lonet.api.response.ApiResponse

class LoggingRequestHandler: IRequestHandler {

    private val handler = LoNet.requestHandler

    override fun performRequest(request: ApiRequest, context: IContext): ApiResponse {
        Log.i("[RequestHandler]", "perform request with ${request.requests.size} request blocks")
        return handler.performRequest(request, context)
    }
}