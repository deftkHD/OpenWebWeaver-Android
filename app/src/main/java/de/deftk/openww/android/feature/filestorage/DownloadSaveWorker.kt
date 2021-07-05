package de.deftk.openww.android.feature.filestorage

import android.content.Context
import android.net.Uri
import androidx.work.*
import de.deftk.openww.android.R
import de.deftk.openww.android.feature.AbstractNotifyingWorker
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.roundToInt

class DownloadSaveWorker(context: Context, params: WorkerParameters) :
    AbstractNotifyingWorker(
        context,
        params,
        NOTIFICATION_CHANNEL_ID,
        NOTIFICATION_ID,
        R.string.notification_download_title,
        R.string.notification_download_content,
        R.drawable.ic_cloud_download_24
    ) {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "notification_channel_download"
        private const val NOTIFICATION_ID = 43

        // input
        private const val DATA_DOWNLOAD_URL = "data_download_url"
        private const val DATA_DESTINATION_URI = "data_destination_uri"
        private const val DATA_FILE_NAME = "data_file_name"
        private const val DATA_FILE_SIZE = "data_file_size"

        fun createRequest(destinationUrl: String, downloadUrl: String, file: IRemoteFile): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<DownloadSaveWorker>()
                .setInputData(
                    workDataOf(
                        DATA_DESTINATION_URI to destinationUrl,
                        DATA_DOWNLOAD_URL to downloadUrl,
                        DATA_FILE_NAME to file.name,
                        DATA_FILE_SIZE to file.size
                    )
                )
                .build()
        }

    }

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(DATA_DOWNLOAD_URL) ?: return exceptionResult(IllegalArgumentException("No download url"))
        val fileName = inputData.getString(DATA_FILE_NAME) ?: return exceptionResult(IllegalArgumentException("No file name"))
        val destinationUri = Uri.parse(inputData.getString(DATA_DESTINATION_URI) ?: return exceptionResult(IllegalArgumentException("No destination url")))
        val fileSize = inputData.getLong(DATA_FILE_SIZE, -1L)
        if (fileSize == -1L)
            return exceptionResult(IllegalArgumentException("Invalid size"))

        setForeground(createForegroundInfo(fileName))

        return withContext(Dispatchers.IO) {
            try {
                val outputStream = applicationContext.contentResolver.openOutputStream(destinationUri, "w") ?: return@withContext exceptionResult(IllegalArgumentException("Destination not found"))
                val inputStream = URL(downloadUrl).openStream()
                val buffer = ByteArray(8192)
                var writtenBytes = 0
                while (!isStopped) {
                    val read = inputStream.read(buffer)
                    writtenBytes += read
                    updateProgress(((writtenBytes.toFloat() / fileSize.toFloat()) * 100).roundToInt(), fileName)
                    if (read <= 0)
                        break
                    outputStream.write(buffer, 0, read)
                }
                outputStream.close()
                inputStream.close()
                if (isStopped)
                    return@withContext exceptionResult(IllegalStateException("Stopped"))

                Result.success() //TODO show permanent notification
            } catch (e: Exception) {
                e.printStackTrace()
                updateProgress(-1, fileName)
                exceptionResult(e)
            }
        }
    }

}
