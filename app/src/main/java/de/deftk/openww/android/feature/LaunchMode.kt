package de.deftk.openww.android.feature

import android.accounts.AccountManager
import android.content.Intent

enum class LaunchMode {

    DEFAULT,
    EMAIL,
    AUTHENTICATOR,
    FILE_UPLOAD;

    companion object {

        fun getLaunchMode(intent: Intent): LaunchMode {
            if (intent.data?.scheme == "mailto")
                return EMAIL
            if (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE)
                return FILE_UPLOAD
            if (intent.hasExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE))
                return AUTHENTICATOR
            return DEFAULT
        }

    }

}