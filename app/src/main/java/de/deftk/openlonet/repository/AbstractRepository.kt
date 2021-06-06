package de.deftk.openlonet.repository

import de.deftk.openlonet.api.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class AbstractRepository {

    suspend fun <T> apiCall(apiCall: suspend () -> T) : Response<T> {
        return withContext(Dispatchers.IO) {
            try {
                Response.Success(apiCall.invoke())
            } catch (e: Exception) {
                Response.Failure(e)
            }
        }
    }

}