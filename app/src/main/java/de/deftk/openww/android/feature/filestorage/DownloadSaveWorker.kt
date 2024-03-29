package de.deftk.openww.android.feature.filestorage

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.deftk.openww.android.R
import de.deftk.openww.android.feature.AbstractNotifyingWorker
import de.deftk.openww.android.notification.Notifications
import de.deftk.openww.android.utils.FileUtil
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import de.deftk.openww.api.model.feature.mailbox.IAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.roundToInt

class DownloadSaveWorker(context: Context, params: WorkerParameters) :
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

        fun createRequest(destinationUrl: String, downloadUrl: String, attachment: IAttachment): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<DownloadSaveWorker>()
                .setInputData(
                    workDataOf(
                        DATA_DESTINATION_URI to destinationUrl,
                        DATA_DOWNLOAD_URL to downloadUrl,
                        DATA_FILE_NAME to attachment.name,
                        DATA_FILE_SIZE to attachment.size.toLong()
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
                val buffer = ByteArray(1024 * 64)
                var writtenBytes = 0
                while (!isStopped) {
                    val read = inputStream.read(buffer)
                    writtenBytes += read
                    updateProgress(((writtenBytes.toFloat() / fileSize.toFloat()) * 100).roundToInt(), writtenBytes, fileSize.toInt(), fileName)
                    if (read <= 0)
                        break
                    outputStream.write(buffer, 0, read)
                }
                outputStream.close()
                inputStream.close()
                if (isStopped)
                    return@withContext exceptionResult(IllegalStateException("Stopped"))

                showDownloadFinishedNotification(fileName, destinationUri)
                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                updateProgress(-1, 0, 1, fileName)
                exceptionResult(e)
            }
        }
    }

    private fun showDownloadFinishedNotification(filename: String, fileUri: Uri) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // create notification channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Notifications.DOWNLOAD_FINISHED_NOTIFICATION_CHANNEL_ID,
                applicationContext.getString(R.string.notification_channel_download_finished_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = applicationContext.getString(R.string.notification_channel_download_finished_description)
            notificationManager.createNotificationChannel(channel)
        }

        var flags = PendingIntent.FLAG_ONE_SHOT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        val intent = FileUtil.getFileOpenIntent(filename, fileUri, applicationContext)
        val openIntent = PendingIntent.getActivity(applicationContext, 0, intent, flags)

        val notification = NotificationCompat.Builder(applicationContext, Notifications.DOWNLOAD_FINISHED_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cloud_download_24)
            .setContentTitle(applicationContext.getString(R.string.download_finished_notification_title))
            .setContentText(applicationContext.getString(R.string.download_finished_notification_content).format(filename))
            .addAction(R.drawable.ic_cloud_download_24, applicationContext.getString(R.string.open), openIntent)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(Notifications.DOWNLOAD_FINISHED_NOTIFICATION_ID, notification)
    }

}
