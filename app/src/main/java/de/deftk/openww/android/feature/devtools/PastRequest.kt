package de.deftk.openww.android.feature.devtools

import de.deftk.openww.api.request.ApiRequest
import de.deftk.openww.api.response.ApiResponse
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Date

data class PastRequest(val id: Int, val request: ApiRequest, val response: ApiResponse, val responseDate: Date) {

    fun getTitle(): String {
        return "Request #$id (HTTP ${response.code})"
    }

    fun getRequestSummary(): List<String> {
        val summary = mutableListOf<String>()
        var currentScope: String
        request.requests.forEach { request ->
            currentScope = "global"
            request.forEach { methodObj ->
                val method = methodObj["method"]?.jsonPrimitive?.content
                if (method == "set_focus") {
                    val params = methodObj["params"]?.jsonObject!!
                    val obj = params["object"]?.jsonPrimitive?.content
                    currentScope = obj ?: "global"
                } else {
                    summary.add("$currentScope/$method")
                }
            }
        }
        return summary
    }

}