package de.deftk.openww.android.feature.filestorage

import android.content.Context
import androidx.core.content.FileProvider
import androidx.work.*
import de.deftk.openww.android.R
import de.deftk.openww.android.feature.AbstractNotifyingWorker
import de.deftk.openww.android.notification.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import kotlin.math.roundToInt

class DownloadOpenWorker(context: Context, params: WorkerParameters) :
    AbstractNotifyingWorker(
        context,
        params,
        Notifications.PROGRESS_NOTIFICATION_CHANNEL_ID,
        Notifications.DOWNLOAD_PROGRESS_NOTIFICATION_ID,
        R.string.notification_download_title,
        R.string.notification_download_content,
        R.drawable.ic_cloud_download_24
    ) {

    companion object {
        // input
        private const val DATA_DOWNLOAD_URL = "data_download_url"
        private const val DATA_DESTINATION_URI = "data_destination_uri"
        private const val DATA_FILE_SIZE = "data_file_size"

        // output
        const val DATA_FILE_URI = "data_file_uri"

        // io
        const val DATA_FILE_NAME = "data_file_name"

        fun createRequest(destinationUrl: String, downloadUrl: String, name: String, size: Long): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<DownloadOpenWorker>()
                .setInputData(
                    workDataOf(
                        DATA_DESTINATION_URI to destinationUrl,
                        DATA_DOWNLOAD_URL to downloadUrl,
                        DATA_FILE_NAME to name,
                        DATA_FILE_SIZE to size
                    )
                )
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(DATA_DOWNLOAD_URL) ?: return exceptionResult(IllegalArgumentException("No download url"))
        val fileName = inputData.getString(DATA_FILE_NAME) ?: return exceptionResult(IllegalArgumentException("No file name"))
        val destinationUri = inputData.getString(DATA_DESTINATION_URI) ?: return exceptionResult(IllegalArgumentException("No destination url"))
        val fileSize = inputData.getLong(DATA_FILE_SIZE, -1L)
        if (fileSize == -1L)
            return exceptionResult(IllegalArgumentException("Invalid size"))

        setForeground(createForegroundInfo(fileName))

        val file = File(destinationUri)
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = URL(downloadUrl).openStream() ?: return@withContext Result.failure()
                val outputStream = file.outputStream()

                var bytesCopied: Long = 0
                val buffer = ByteArray(1024 * 64)
                var bytes = inputStream.read(buffer)
                while (bytes >= 0 && !isStopped) {
                    outputStream.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    bytes = inputStream.read(buffer)
                    updateProgress(((bytesCopied.toFloat() / fileSize.toFloat()) * 100).roundToInt(), bytesCopied.toInt(), fileSize.toInt(), fileName)
                }
                inputStream.close()
                if (isStopped) {
                    try {
                        file.delete()
                    } catch (ignored: Exception) { }
                    return@withContext exceptionResult(IllegalStateException("Stopped"))
                }

                val fileUri = FileProvider.getUriForFile(applicationContext, applicationContext.getString(R.string.file_provider_authority), file)
                Result.success(workDataOf(DATA_FILE_URI to fileUri.toString(), DATA_FILE_NAME to fileName))
            } catch (e: Exception) {
                try {
                    file.delete()
                } catch (ignored: Exception) { }
                updateProgress(-1, 0, 1, fileName)
                e.printStackTrace()
                Result.failure(workDataOf(DATA_ERROR_MESSAGE to (e.localizedMessage ?: e.message ?: e.toString())))
            }
        }
    }
}