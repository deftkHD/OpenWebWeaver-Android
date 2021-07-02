package de.deftk.openww.android.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import java.lang.Exception

object Reporter {

    fun reportException(@StringRes message: Int, exception: Exception, context: Context) {
        reportException(message, exception.localizedMessage ?: exception.message ?: exception.toString(), context)
        exception.printStackTrace()
    }

    fun reportException(@StringRes message: Int, exception: String, context: Context) {
        Toast.makeText(context, context.getString(message).format(), Toast.LENGTH_LONG).show()
    }

}