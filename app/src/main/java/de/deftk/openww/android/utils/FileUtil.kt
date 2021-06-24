package de.deftk.openww.android.utils

import android.webkit.MimeTypeMap

object FileUtil {

    fun getMimeType(filename: String): String {
        val index = filename.lastIndexOf('.')
        if (index == -1 || index + 1 >= filename.length)
            return ""
        val extension = filename.substring(index + 1)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: ""
    }

}