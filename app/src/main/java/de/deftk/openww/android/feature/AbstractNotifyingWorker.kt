package de.deftk.openww.android.feature

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import de.deftk.openww.android.R

abstract class AbstractNotifyingWorker(
    context: Context,
    params: WorkerParameters,
    private val notificationChannelId: String,
    private val notificationId: Int,
    private val notificationTitleResource: Int,
    private val notificationContentResource: Int,
    private val notificationIcon: Int
) : CoroutineWorker(context, params) {

    companion object {
        const val ARGUMENT_PROGRESS = "argument_progress"

        // output
        const val DATA_ERROR_MESSAGE = "data_error_message"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    protected suspend fun updateProgress(progress: Int, fileName: String) {
        setProgress(workDataOf(ARGUMENT_PROGRESS to progress))
        notificationManager.notify(notificationId, buildNotification(fileName, progress))
    }

    protected fun createForegroundInfo(fileName: String): ForegroundInfo {
        //TODO notification groups

        // create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(notificationChannelId, applicationContext.getString(
                    R.string.notification_channel_upload_name
                ), NotificationManager.IMPORTANCE_LOW)
            channel.description = applicationContext.getString(R.string.notification_channel_upload_description)
            notificationManager.createNotificationChannel(channel)
        }

        return ForegroundInfo(notificationId, buildNotification(fileName, 0))
    }

    private fun buildNotification(fileName: String, progress: Int): Notification {
        val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        val builder = NotificationCompat.Builder(applicationContext, notificationChannelId)
            .setContentTitle(applicationContext.getString(notificationTitleResource))
            .setTicker(applicationContext.getString(notificationTitleResource))
            .setContentText(applicationContext.getString(notificationContentResource).format(fileName))
            .setSmallIcon(notificationIcon)
            .addAction(R.drawable.ic_cancel_24, applicationContext.getString(R.string.cancel), intent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(100, progress, false)

        return builder.build()
    }

    protected fun exceptionResult(ex: Exception): Result {
        return Result.failure(workDataOf(DATA_ERROR_MESSAGE to (ex.localizedMessage ?: ex.message ?: ex.toString())))
    }

}