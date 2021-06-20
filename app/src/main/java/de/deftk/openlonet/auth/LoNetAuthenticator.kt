package de.deftk.openlonet.auth

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import de.deftk.lonet.api.LoNetClient
import de.deftk.lonet.api.auth.Credentials
import de.deftk.openlonet.activities.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoNetAuthenticator(private val context: Context): AbstractAccountAuthenticator(context) {

    companion object {
        private val TAG = LoNetAuthenticator::class.java.name

        const val ACCOUNT_TYPE = "OpenLoNet/lo-net2.de"
        const val EXTRA_TOKEN_TYPE = "OpenLoNetApiToken"
    }

    override fun getAccountRemovalAllowed(response: AccountAuthenticatorResponse, account: Account): Bundle? {
        val accountManager = AccountManager.get(context)
        val token = accountManager.getPassword(account)
        val apiContext = try {
            LoNetClient.login(Credentials.fromToken(account.name, token))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister account online")
            e.printStackTrace()
            null
        }
        CoroutineScope(Dispatchers.IO).launch {
            apiContext?.getUser()?.logoutDestroyToken(token, apiContext.getUser().getRequestContext(apiContext))

            val bundle = Bundle()
            bundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true)
            response.onResult(bundle)
        }

        response.onRequestContinued()
        return null
    }

    override fun editProperties(response: AccountAuthenticatorResponse, accountType: String?): Bundle {
        throw IllegalStateException("Not supported")
    }

    override fun addAccount(response: AccountAuthenticatorResponse, accountType: String, authTokenType: String?, requiredFeatures: Array<out String>?, options: Bundle): Bundle {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(ACCOUNT_TYPE, accountType)
        intent.putExtra(EXTRA_TOKEN_TYPE, authTokenType)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun confirmCredentials(response: AccountAuthenticatorResponse, account: Account?, options: Bundle): Bundle {
        throw IllegalStateException("Not supported")
    }

    override fun getAuthToken(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?): Bundle {
        throw IllegalStateException("Not supported")
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