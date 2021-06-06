package de.deftk.openlonet.feature.filestorage

import android.content.Context
import androidx.core.content.FileProvider
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.deftk.lonet.api.model.feature.filestorage.IRemoteFile
import de.deftk.openlonet.R
import de.deftk.openlonet.feature.AbstractNotifyingWorker
import de.deftk.openlonet.fragments.feature.filestorage.FilesFragment
import java.io.File
import java.net.URL
import kotlin.math.roundToInt

class DownloadOpenWorker(context: Context, params: WorkerParameters) :
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
        const val DATA_DOWNLOAD_URL = "data_download_url"
        const val DATA_DESTINATION_URI = "data_destination_uri"
        const val DATA_FILE_SIZE = "data_file_size"

        // output
        const val DATA_FILE_URI = "data_file_uri"

        // io
        const val DATA_FILE_NAME = "data_file_name"

        fun createRequest(destinationUrl: String, downloadUrl: String, file: IRemoteFile): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<DownloadOpenWorker>()
                .setInputData(
                    workDataOf(
                        DATA_DESTINATION_URI to destinationUrl,
                        DATA_DOWNLOAD_URL to downloadUrl,
                        DATA_FILE_NAME to file.name,
                        DATA_FILE_SIZE to file.getSize()
                    )
                )
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(DownloadSaveWorker.DATA_DOWNLOAD_URL) ?: return Result.failure()
        val fileName = inputData.getString(DownloadSaveWorker.DATA_FILE_NAME) ?: return Result.failure()
        val destinationUri = inputData.getString(DownloadSaveWorker.DATA_DESTINATION_URI) ?: return Result.failure()
        val fileSize = inputData.getLong(DownloadSaveWorker.DATA_FILE_SIZE, -1L)
        if (fileSize == -1L)
            return Result.failure()

        setForeground(createForegroundInfo(fileName))

        try {
            val file = File(destinationUri)
            val inputStream = URL(downloadUrl).openStream() ?: return Result.failure()
            val outputStream = file.outputStream()

            var bytesCopied: Long = 0
            val buffer = ByteArray(8192)
            var bytes = inputStream.read(buffer)
            while (bytes >= 0 && !isStopped) {
                outputStream.write(buffer, 0, bytes)
                bytesCopied += bytes
                bytes = inputStream.read(buffer)
                updateProgress(((bytesCopied.toFloat() / fileSize.toFloat()) * 100).roundToInt(), fileName)
            }
            inputStream.close()
            val fileUri = FileProvider.getUriForFile(applicationContext, FilesFragment.FILE_PROVIDER_AUTHORITY, file)
            return Result.success(workDataOf(DATA_FILE_URI to fileUri.toString(), DATA_FILE_NAME to fileName))
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}