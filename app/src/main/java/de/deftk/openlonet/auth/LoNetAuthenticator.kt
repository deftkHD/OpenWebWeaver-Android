package de.deftk.openlonet.auth

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import de.deftk.lonet.api.LoNetClient
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.activities.LoginActivity
import de.deftk.openlonet.activities.MainActivity

class LoNetAuthenticator(private val context: Context): AbstractAccountAuthenticator(context) {

    override fun getAccountRemovalAllowed(response: AccountAuthenticatorResponse?, account: Account): Bundle {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_LOGOUT, account.name)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun editProperties(response: AccountAuthenticatorResponse, accountType: String?): Bundle {
        throw IllegalStateException("Not supported")
    }

    override fun addAccount(response: AccountAuthenticatorResponse, accountType: String, authTokenType: String?, requiredFeatures: Array<out String>?, options: Bundle): Bundle {
        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(AuthStore.ACCOUNT_TYPE, accountType)
        intent.putExtra(AuthStore.EXTRA_TOKEN_TYPE, authTokenType)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun confirmCredentials(response: AccountAuthenticatorResponse, account: Account?, options: Bundle): Bundle {
        throw IllegalStateException("Not supported")
    }

    override fun getAuthToken(response: AccountAuthenticatorResponse, account: Account, authTokenType: String?, options: Bundle): Bundle {
        val accountManager = AccountManager.get(context)
        val result = try {
            //LoNetClient.loginCreateToken(account.name, accountManager.getPassword(account), "OpenLoNet", "${Build.BRAND} ${Build.MODEL}")
            LoNetClient.loginToken(account.name, accountManager.getPassword(account))
        } catch (e: Exception) {
            val bundle = Bundle()
            bundle.putString(AccountManager.KEY_ERROR_CODE, e::class.java.simpleName)
            bundle.putString(AccountManager.KEY_ERROR_MESSAGE, e.message)
            return bundle
        }
        AuthStore.setApiContext(result)
        val sessionId = result.getSessionId()

        if (sessionId.isNotEmpty()) {
            val bundle = Bundle()
            bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
            bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
            bundle.putString(AccountManager.KEY_AUTHTOKEN, sessionId)
            return bundle
        }

        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(AuthStore.ACCOUNT_TYPE, account.type)
        intent.putExtra(AuthStore.EXTRA_TOKEN_TYPE, authTokenType)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun getAuthTokenLabel(authTokenType: String?): String {
        return "lo-net2"
    }

    override fun updateCredentials(response: AccountAuthenticatorResponse, account: Account?, authTokenType: String?, options: Bundle): Bundle {
        throw IllegalStateException("Not supported")
    }

    override fun hasFeatures(response: AccountAuthenticatorResponse, account: Account?, features: Array<out String>?): Bundle {
        throw IllegalStateException("Not supported")
    }
}