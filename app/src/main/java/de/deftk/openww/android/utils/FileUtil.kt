package de.deftk.openww.android.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.preference.PreferenceManager

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

}