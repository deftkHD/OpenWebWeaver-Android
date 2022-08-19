package de.deftk.openww.android.utils

import android.app.Activity
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

object AndroidUtil {

    fun hideKeyboard(activity: Activity, view: View) {
        WindowCompat.getInsetsController(activity.window, view).hide(
            WindowInsetsCompat.Type.ime())
    }

}