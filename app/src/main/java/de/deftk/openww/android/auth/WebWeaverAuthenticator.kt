package de.deftk.openww.android.auth

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import de.deftk.openww.android.R
import de.deftk.openww.android.activities.MainActivity
import de.deftk.openww.api.WebWeaverClient
import de.deftk.openww.api.auth.Credentials
import kotlinx.coroutines.runBlocking

class WebWeaverAuthenticator(private val context: Context): AbstractAccountAuthenticator(context) {

    companion object {
        private val TAG = WebWeaverAuthenticator::class.java.name
    }

    override fun getAccountRemovalAllowed(response: AccountAuthenticatorResponse, account: Account): Bundle? {
        val accountManager = AccountManager.get(context)
        val token = accountManager.getPassword(account)
        val apiContext = try {
            runBlocking {
                WebWeaverClient.login(Credentials.fromToken(account.name, token))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister account online")
            e.printStackTrace()
            response.onError(0, e.localizedMessage ?: e.message ?: e.toString())
            return null
        }
        runBlocking {
            apiContext.user.logoutDestroyToken(token, apiContext.user.getRequestContext(apiContext))

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
        check(accountType == context.getString(R.string.account_type))
        intent.putExtra(AuthHelper.EXTRA_TOKEN_TYPE, authTokenType)
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
        return "WebWeaver"
    }

    override fun updateCredentials(response: AccountAuthenticatorResponse, account: Account?, authTokenType: String?, options: Bundle): Bundle {
        throw IllegalStateException("Not supported")
    }

    override fun hasFeatures(response: AccountAuthenticatorResponse, account: Account?, features: Array<out String>?): Bundle {
        throw IllegalStateException("Not supported")
    }
}