package de.deftk.openww.android.feature

import android.content.Intent

enum class LaunchMode {

    DEFAULT,
    EMAIL;

    companion object {

        fun getLaunchMode(intent: Intent): LaunchMode {
            if (intent.data?.scheme == "mailto")
                return EMAIL
            return DEFAULT
        }

    }

}