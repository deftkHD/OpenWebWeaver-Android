package de.deftk.openlonet

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import de.deftk.lonet.api.LoNetClient
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.lonet.api.implementation.User
import de.deftk.lonet.api.model.IRequestContext
import de.deftk.openlonet.activities.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

object AuthStore {

    private const val LOG_TAG = "AuthStore"
    const val ACCOUNT_TYPE = "OpenLoNet/lo-net2.de"
    const val EXTRA_TOKEN_TYPE = "de.deftk.openlonet.auth.extra_token_type"

    var currentApiToken: String? = null
    var currentAccount: Account? = null

    private var apiContext: ApiContext? = null

    fun getApiUser(): User {
        return getApiContext().getUser()
    }

    fun getUserContext(): IRequestContext {
        return getApiUser().getRequestContext(getApiContext())
    }

    fun getApiContext(): ApiContext {
        return apiContext ?: error("No user logged in")
    }

    fun setApiContext(context: ApiContext) {
        apiContext = context
    }

    fun isUserLoggedIn(): Boolean {
        return apiContext != null
    }

    /**
     * @return if login was successful or if a new activity was launched
     */
    suspend fun performLogin(context: Context): Boolean {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE)
        when {
            accounts.isNotEmpty() -> {
                // use account
                currentAccount = accounts[0]
                currentApiToken = accountManager.blockingGetAuthToken(currentAccount, ACCOUNT_TYPE, true)
                try {
                    apiContext = LoNetClient.loginToken(accounts[0].name, currentApiToken ?: error("No token provided!"), false, ApiContext::class.java)
                    return true
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Login failed")
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        if (e is IOException) {
                            when (e) {
                                is UnknownHostException -> Toast.makeText(context, context.getString(R.string.request_failed_connection), Toast.LENGTH_LONG).show()
                                is TimeoutException -> Toast.makeText(context, context.getString(R.string.request_failed_timeout), Toast.LENGTH_LONG).show()
                                else -> Toast.makeText(context, String.format(context.getString(R.string.request_failed_other), e.message), Toast.LENGTH_LONG).show()
                            }
                        } else {
                            accountManager.invalidateAuthToken(ACCOUNT_TYPE, currentApiToken)
                            Toast.makeText(context, context.getString(R.string.token_expired), Toast.LENGTH_LONG).show()

                            val intent = Intent(context, LoginActivity::class.java)
                            val savedUser = currentAccount?.name
                            if (savedUser != null) {
                                intent.putExtra(LoginActivity.EXTRA_LOGIN, savedUser)
                            }
                            context.startActivity(intent)
                        }
                    }
                }
            }
            else -> {
                // add new account or simply login without adding an account
                val intent = Intent(context, LoginActivity::class.java)
                withContext(Dispatchers.Main) {
                    context.startActivity(intent)
                }
            }
        }
        return false
    }

}