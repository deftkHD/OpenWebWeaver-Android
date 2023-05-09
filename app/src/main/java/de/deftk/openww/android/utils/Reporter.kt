package de.deftk.openww.android.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import de.deftk.openww.android.R
import de.deftk.openww.android.feature.devtools.ExceptionSource

object Reporter {

    fun reportUncaughtException(exception: Throwable, context: Context) {
        if (DebugUtil.areDevToolsEnabled(context)) {
            DebugUtil.reportException(ExceptionSource.UNCAUGHT_EXCEPTION, Thread.currentThread().stackTrace, exception, null)
        }
        val exStr = exception.localizedMessage ?: exception.message ?: exception.toString()
        Toast.makeText(context, context.getString(R.string.uncaught_exception).format(exStr), Toast.LENGTH_LONG).show()
        exception.printStackTrace()
    }

    fun reportException(@StringRes message: Int, exception: Throwable, context: Context) {
        if (DebugUtil.areDevToolsEnabled(context)) {
            DebugUtil.reportException(ExceptionSource.CAUGHT_EXCEPTION, Thread.currentThread().stackTrace, exception, null)
        }
        val exStr = exception.localizedMessage ?: exception.message ?: exception.toString()
        Toast.makeText(context, context.getString(message).format(exStr), Toast.LENGTH_LONG).show()
        exception.printStackTrace()
    }

    fun reportException(@StringRes message: Int, exception: String?, context: Context) {
        val text = if (exception != null) context.getString(message).format(exception) else context.getString(message)
        if (DebugUtil.areDevToolsEnabled(context)) {
            DebugUtil.reportException(ExceptionSource.ASSERTION, Thread.currentThread().stackTrace, null, text)
        }
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    fun reportFeatureNotAvailable(context: Context) {
        if (DebugUtil.areDevToolsEnabled(context)) {
            DebugUtil.reportException(ExceptionSource.PERMISSION_CHECK, Thread.currentThread().stackTrace, null, context.getString(R.string.feature_not_available))
        }
        Toast.makeText(context, R.string.feature_not_available, Toast.LENGTH_LONG).show()
    }

}