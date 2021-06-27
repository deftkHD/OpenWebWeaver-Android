package de.deftk.openww.android.utils

import android.content.SharedPreferences
import android.webkit.MimeTypeMap

object FileUtil {

    fun getMimeType(filename: String): String {
        val index = filename.lastIndexOf('.')
        if (index == -1 || index + 1 >= filename.length)
            return ""
        val extension = filename.substring(index + 1)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: ""
    }

    fun escapeFileName(name: String): String {
        return name.map {
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
        }.toString()
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

}