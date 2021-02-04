package de.deftk.openlonet.feature.filestorage

import android.content.Context
import android.net.Uri
import androidx.work.WorkerParameters
import de.deftk.openlonet.R
import de.deftk.openlonet.feature.AbstractNotifyingWorker
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

        const val DATA_DOWNLOAD_URL = "data_download_url"
        const val DATA_DESTINATION_URI = "data_destination_uri"
        const val DATA_FILE_NAME = "data_file_name"
        const val DATA_FILE_SIZE = "data_file_size"
    }

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(DATA_DOWNLOAD_URL) ?: return Result.failure()
        val fileName = inputData.getString(DATA_FILE_NAME) ?: return Result.failure()
        val destinationUri = Uri.parse(inputData.getString(DATA_DESTINATION_URI) ?: return Result.failure())
        val fileSize = inputData.getLong(DATA_FILE_SIZE, -1L)
        if (fileSize == -1L)
            return Result.failure()

        setForeground(createForegroundInfo(fileName))

        try {
            val outputStream = applicationContext.contentResolver.openOutputStream(destinationUri, "w") ?: return Result.failure()
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
                return Result.failure()

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}
