package de.deftk.openlonet.utils

import android.webkit.MimeTypeMap

object FileUtil {

    fun getMimeType(filename: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(filename)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: ""
    }

}