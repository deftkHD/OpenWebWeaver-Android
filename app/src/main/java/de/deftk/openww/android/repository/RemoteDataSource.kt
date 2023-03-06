package de.deftk.openww.android.repository

import de.deftk.openww.android.api.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class RemoteDataSource {

    suspend fun <T> apiCallWithResponse(apiCall: suspend () -> T) : Response<T> {
        return withContext(Dispatchers.IO) {
            try {
                Response.Success(apiCall.invoke())
            } catch (e: Exception) {
                Response.Failure(e)
            }
        }
    }

    suspend fun <T> apiCall(apiCall: suspend () -> T) : T {
        return withContext(Dispatchers.IO) {
            apiCall.invoke()
        }
    }

}