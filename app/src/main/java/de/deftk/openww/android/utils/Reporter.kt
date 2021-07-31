package de.deftk.openww.android.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import de.deftk.openww.android.R
import java.lang.Exception

object Reporter {

    fun reportException(@StringRes message: Int, exception: Exception, context: Context) {
        reportException(message, exception.localizedMessage ?: exception.message ?: exception.toString(), context)
        exception.printStackTrace()
    }

    fun reportException(@StringRes message: Int, exception: String, context: Context) {
        Toast.makeText(context, context.getString(message).format(exception), Toast.LENGTH_LONG).show()
    }

    fun reportFeatureNotAvailable(context: Context) {
        Toast.makeText(context, R.string.feature_not_available, Toast.LENGTH_LONG).show()
    }

}