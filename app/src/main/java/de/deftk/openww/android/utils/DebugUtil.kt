package de.deftk.openww.android.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import de.deftk.openww.android.feature.devtools.ExceptionReport
import de.deftk.openww.android.feature.devtools.ExceptionSource
import java.util.*

object DebugUtil {

    private var nextId = 0
    private val _exceptions = MutableLiveData<MutableList<ExceptionReport>>()
    val exceptions: LiveData<MutableList<ExceptionReport>> = _exceptions

    fun reportException(source: ExceptionSource, stackTrace: Array<StackTraceElement>, exception: Throwable?, message: String?) {
        Log.e("DebugUtil", "Received exception report!")
        val trace = stackTrace.toMutableList()
        trace.removeFirst()
        trace.removeFirst()
        val report = ExceptionReport(nextId++, source, Date(), trace, exception, message)
        val list = _exceptions.value ?: mutableListOf()
        list.add(report)
        _exceptions.value = list
    }

    fun areDevToolsEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_devtools", false)
    }

}