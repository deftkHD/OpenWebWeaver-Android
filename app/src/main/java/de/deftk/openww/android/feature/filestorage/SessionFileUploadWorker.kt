package de.deftk.openww.android.feature.filestorage

import android.content.Context
import android.net.Uri
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.deftk.openww.android.R
import de.deftk.openww.android.feature.AbstractNotifyingWorker
import de.deftk.openww.android.notification.Notifications
import de.deftk.openww.api.WebWeaverClient
import de.deftk.openww.api.implementation.RequestContext
import de.deftk.openww.api.implementation.feature.filestorage.session.SessionFile
import de.deftk.openww.api.model.IRequestContext
import de.deftk.openww.api.request.UserApiRequest
import de.deftk.openww.api.request.handler.DefaultRequestHandler
import de.deftk.openww.api.response.ResponseUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.math.roundToInt

class SessionFileUploadWorker(context: Context, params: WorkerParameters) : AbstractNotifyingWorker(
    context,
    params,
    Notifications.PROGRESS_NOTIFICATION_CHANNEL_ID,
    Notifications.UPLOAD_PROGRESS_NOTIFICATION_ID,
    R.string.notification_upload_title,
    R.string.notification_upload_content,
    R.drawable.ic_cloud_upload_24
) {

    companion object {
        // input
        private const val DATA_FILE_URI = "data_file_uri"
        private const val DATA_FILE_NAME = "data_file_name"
        private const val DATA_CONTEXT_LOGIN = "data_context_login"
        private const val DATA_CONTEXT_SESSION_ID = "data_session_id"
        private const val DATA_CONTEXT_REQUEST_URL = "data_context_request_url"

        // output
        const val DATA_SESSION_FILE = "data_session_file"

        fun createRequest(fileUri: Uri, fileName: String, requestContext: IRequestContext): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<SessionFileUploadWorker>()
                .setInputData(
                    workDataOf(
                        DATA_FILE_URI to fileUri.toString(),
                        DATA_FILE_NAME to fileName,
                        DATA_CONTEXT_LOGIN to requestContext.login,
                        DATA_CONTEXT_REQUEST_URL to requestContext.requestUrl,
                        DATA_CONTEXT_SESSION_ID to requestContext.sessionId
                    )
                )
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val uri = Uri.parse(inputData.getString(DATA_FILE_URI) ?: return exceptionResult(IllegalArgumentException("No file uri")))
        val fileName = inputData.getString(DATA_FILE_NAME) ?: return exceptionResult(IllegalArgumentException("No file name"))
        val login = inputData.getString(DATA_CONTEXT_LOGIN) ?: return exceptionResult(IllegalArgumentException("No login"))
        val sessionId = inputData.getString(DATA_CONTEXT_SESSION_ID) ?: return exceptionResult(IllegalArgumentException("No session id"))
        val requestUrl = inputData.getString(DATA_CONTEXT_REQUEST_URL) ?: return exceptionResult(IllegalArgumentException("No request url"))
        val requestContext = RequestContext(login, sessionId, requestUrl, DefaultRequestHandler())

        setForeground(createForegroundInfo(fileName))

        return withContext(Dispatchers.IO) {
            try {
                val fileSize = applicationContext.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: return@withContext exceptionResult(IllegalStateException("Failed to open file descriptor"))
                val inputStream = applicationContext.contentResolver.openInputStream(uri) ?: return@withContext exceptionResult(IllegalStateException("Failed to open file input stream"))
                val sessionFile = addSessionFile(fileName, requestContext)

                val buffer = ByteArray(1024 * 64)
                var writtenBytes = 0
                while (!isStopped) {
                    val read = inputStream.read(buffer)
                    if (read <= 0) break
                    if (read != buffer.size) {
                        val newBuffer = ByteArray(read)
                        System.arraycopy(buffer, 0, newBuffer, 0, read)
                        sessionFile.append(newBuffer, requestContext)
                    } else {
                        sessionFile.append(buffer, requestContext)
                    }
                    writtenBytes += read
                    val p = writtenBytes.toFloat() / fileSize.toFloat()
                    updateProgress((p * 100).roundToInt(), writtenBytes, fileSize.toInt(), fileName)
                }
                if (isStopped) {
                    sessionFile.delete(requestContext)
                }

                Result.success(workDataOf(DATA_SESSION_FILE to WebWeaverClient.json.encodeToString(sessionFile)))
            } catch (e: Exception) {
                updateProgress(-1, 0, 1, fileName)
                e.printStackTrace()
                Result.failure(workDataOf(DATA_ERROR_MESSAGE to (e.localizedMessage ?: e.message ?: e.toString())))
            }
        }
    }

    private suspend fun addSessionFile(name: String, requestContext: IRequestContext): SessionFile {
        val request = UserApiRequest(requestContext)
        val id = request.addAddSessionFileRequest(name, byteArrayOf())[1]
        val response = request.fireRequest()
        val subResponse = ResponseUtil.getSubResponseResult(response.toJson(), id)
        return WebWeaverClient.json.decodeFromJsonElement(subResponse["file"]!!)
    }

}