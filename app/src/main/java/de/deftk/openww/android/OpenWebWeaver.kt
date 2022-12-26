package de.deftk.openww.android

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import de.deftk.openww.android.utils.Reporter

@HiltAndroidApp
class OpenWebWeaver : Application() {

    override fun onCreate() {
        super.onCreate()

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("devtools_uncaught_exception_handling", false)) {
            Log.i("OpenWebWeaver", "Using alternative app run to handle uncaught exceptions (enabled via DevTools)")
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            val handler = Handler(Looper.getMainLooper())
            Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
                handler.post {
                    throw BackgroundException(throwable)
                }
            }

            while (true)  {
                try {
                    Looper.loop()
                    Thread.setDefaultUncaughtExceptionHandler(defaultHandler)
                    throw RuntimeException("Main thread loop unexpectedly exited")
                } catch (e: Exception) {
                    Reporter.reportUncaughtException(e, this)
                }
            }
        }
    }

}