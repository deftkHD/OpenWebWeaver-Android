package de.deftk.openww.android.feature

import android.accounts.AccountManager
import android.content.Intent

enum class LaunchMode {

    DEFAULT,
    EMAIL,
    AUTHENTICATOR;

    companion object {

        fun getLaunchMode(intent: Intent): LaunchMode {
            if (intent.data?.scheme == "mailto")
                return EMAIL
            if (intent.hasExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE))
                return AUTHENTICATOR
            return DEFAULT
        }

    }

}