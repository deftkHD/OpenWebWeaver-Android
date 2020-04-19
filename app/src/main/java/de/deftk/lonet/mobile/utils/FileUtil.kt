package de.deftk.lonet.mobile.utils

import android.webkit.MimeTypeMap

object FileUtil {

    fun getMimeType(filename: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(filename)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: ""
    }

}