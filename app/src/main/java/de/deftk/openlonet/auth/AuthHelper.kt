package de.deftk.openlonet.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.preference.PreferenceManager

object AuthHelper {

    const val ACCOUNT_TYPE = "OpenLoNet/lo-net2.de"
    const val EXTRA_TOKEN_TYPE = "OpenLoNetApiToken"
    private const val LAST_LOGIN_PREFERENCE = "last_login"

    fun rememberLogin(account: Account, context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(LAST_LOGIN_PREFERENCE, account.name)
            .apply()
    }

    fun getRememberedLogin(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(LAST_LOGIN_PREFERENCE, null)
    }

    fun estimateAuthState(context: Context): AuthState {
        val accounts = findAccounts(null, context)
        return when {
            accounts.size == 1 -> AuthState.SINGLE
            accounts.size > 1 -> AuthState.MULTIPLE
            else -> AuthState.ADD_NEW
        }
    }

    fun findAccounts(prioritizedLogin: String?, context: Context): Array<Account> {
        val lastLogin = getRememberedLogin(context)
        val allAccounts = AccountManager.get(context).getAccountsByType(ACCOUNT_TYPE)
        if (prioritizedLogin != null && allAccounts.any { it.name == prioritizedLogin })
            return allAccounts.filter { it.name == prioritizedLogin }.toTypedArray()
        if (lastLogin == null)
            return allAccounts
        if (allAccounts.any { it.name == lastLogin })
            return allAccounts.filter { it.name == lastLogin }.toTypedArray()
        return allAccounts
    }

    enum class AuthState {
        SINGLE, // simple: just login with the account returned by the findAccounts() helper function
        MULTIPLE, // ask user to choose an account from the list returned by the findAccounts() helper function
        ADD_NEW // user must login with a new account
    }

}