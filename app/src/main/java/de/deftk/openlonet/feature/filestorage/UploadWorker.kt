package de.deftk.openlonet.feature.filestorage

import android.content.Context
import android.net.Uri
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.deftk.lonet.api.LoNetClient
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.feature.AbstractNotifyingWorker
import kotlinx.serialization.encodeToString
import kotlin.math.roundToInt

class UploadWorker(context: Context, params: WorkerParameters) :
    AbstractNotifyingWorker(
        context,
        params,
        NOTIFICATION_CHANNEL_ID,
        NOTIFICATION_ID,
        R.string.notification_upload_title,
        R.string.notification_upload_content,
        R.drawable.ic_cloud_upload_24
    ) {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "notification_channel_upload"
        private const val NOTIFICATION_ID = 42

        const val DATA_FILE_URI = "data_file_uri"
        const val DATA_FILE_NAME = "data_file_name"

        // just hope the obj is not bigger > 1024 bytes
        const val DATA_SESSION_FILE = "data_session_file"
    }

    override suspend fun doWork(): Result {
        val uri = Uri.parse(inputData.getString(DATA_FILE_URI) ?: return Result.failure())
        val fileName = inputData.getString(DATA_FILE_NAME) ?: return Result.failure()

        setForeground(createForegroundInfo(fileName))

        try {
            val fileSize = applicationContext.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: return Result.failure()
            val inputStream = applicationContext.contentResolver.openInputStream(uri) ?: return Result.failure()
            val sessionFile = AuthStore.getApiUser().addSessionFile(fileName, byteArrayOf(), AuthStore.getUserContext())
            val buffer = ByteArray(1024 * 1024)
            var writtenBytes = 0
            while (!isStopped) {
                val read = inputStream.read(buffer)
                if (read < 0) break
                if (read != buffer.size) {
                    val newBuffer = ByteArray(read)
                    System.arraycopy(buffer, 0, newBuffer, 0, read)
                    sessionFile.append(newBuffer, AuthStore.getUserContext())
                } else {
                    sessionFile.append(buffer, AuthStore.getUserContext())
                }
                writtenBytes += read
                val p = writtenBytes.toFloat() / fileSize.toFloat()
                updateProgress((p * 100).roundToInt(), fileName)
            }
            val serializedSessionFile = LoNetClient.json.encodeToString(sessionFile)
            return Result.success(workDataOf(DATA_SESSION_FILE to serializedSessionFile))
        } catch (e: Exception) {
            return Result.failure()
        }
    }

}