package de.deftk.lonet.mobile.utils

import android.os.Build
import android.text.Html
import android.text.Spanned

object TextUtils {

    //TODO internal references
    fun parse(text: String?): Spanned {
        val html = text
            ?.replace("\n", "<br>")
            ?.replace("\t", "&nbsp; &nbsp; &nbsp;")
            ?: ""
        @Suppress("DEPRECATION")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        else
            Html.fromHtml(html)

    }

}