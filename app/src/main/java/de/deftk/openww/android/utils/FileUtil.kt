package de.deftk.openww.android.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import de.deftk.openww.android.R
import de.deftk.openww.android.feature.AbstractNotifyingWorker
import de.deftk.openww.android.feature.filestorage.DownloadOpenWorker
import de.deftk.openww.api.model.feature.FileDownloadUrl
import de.deftk.openww.api.model.feature.FileUrl
import java.io.File

object FileUtil {

    fun getMimeType(filename: String): String {
        val index = filename.lastIndexOf('.')
        if (index == -1 || index + 1 >= filename.length)
            return ""
        val extension = filename.substring(index + 1)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: ""
    }

    fun escapeFileName(name: String): String {
        return String(name.map {
            when (it) {
                '|' -> '_'
                '\\' -> '_'
                '?' -> '_'
                '*' -> '_'
                '<' -> '_'
                '\"' -> '_'
                '>' -> '_'
                ':' -> '_'
                else -> it
            }
        }.toTypedArray().toCharArray())
    }

    fun normalizeFileName(name: String, preferences: SharedPreferences): String {
        return if (preferences.getBoolean("file_storage_correct_file_names", false)) {
            if (name.contains('.')) {
                name.substring(0, name.lastIndexOf('.')).replace("_", " ")
            } else {
                name.replace("_", " ")
            }
        } else {
            name
        }
    }

    fun uriToFileName(uri: Uri, context: Context): String {
        var cursor: Cursor? = null
        var filename = "unknown.bin"
        try {
            cursor = context.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)

            if (cursor?.moveToFirst() == true) {
                val i = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                if (i == -1)
                    error("Failed to find column for display name")
                filename = cursor.getString(i)
            }
        } finally {
            cursor?.close()
        }
        return filename
    }

    fun getFileOpenIntent(fileName: String, fileUri: Uri, context: Context): Intent {
        val mime = getMimeType(fileName)
        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.type = mime
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, normalizeFileName(fileName, PreferenceManager.getDefaultSharedPreferences(context)))
        val viewIntent = Intent(Intent.ACTION_VIEW)
        viewIntent.setDataAndType(fileUri, mime)
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(sendIntent, fileName).apply { putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent)) }
    }

    fun showFileOpenIntent(fileName: String, fileUri: Uri, context: Context) {
        val intent = getFileOpenIntent(fileName, fileUri, context)
        context.startActivity(intent)
    }

    fun openAttachment(fragment: Fragment, url: FileUrl, name: String) {
        val activity = fragment.requireActivity()
        val workManager = WorkManager.getInstance(activity)
        val tempDir = File(activity.cacheDir, "attachments")
        if (!tempDir.exists())
            tempDir.mkdir()
        val tempFile = File(tempDir, escapeFileName(url.name ?: name))
        if (url.size == null) {
            Reporter.reportException(R.string.error_invalid_size, "null", activity)
            return
        }
        val workRequest = DownloadOpenWorker.createRequest(tempFile.absolutePath, url.url, url.name ?: name, url.size!!)
        workManager.enqueue(workRequest)
        workManager.getWorkInfoByIdLiveData(workRequest.id).observe(fragment.viewLifecycleOwner) { workInfo ->
            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                val fileUri = Uri.parse(workInfo.outputData.getString(DownloadOpenWorker.DATA_FILE_URI))
                val fileName = workInfo.outputData.getString(DownloadOpenWorker.DATA_FILE_NAME)!!
                showFileOpenIntent(fileName, fileUri, activity)
            } else if (workInfo.state == WorkInfo.State.FAILED) {
                val errorMessage = workInfo.outputData.getString(AbstractNotifyingWorker.DATA_ERROR_MESSAGE) ?: "Unknown"
                Reporter.reportException(R.string.error_download_worker_failed, errorMessage, activity)
            }
        }
    }

}