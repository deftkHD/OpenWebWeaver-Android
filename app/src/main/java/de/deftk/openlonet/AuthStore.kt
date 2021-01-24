package de.deftk.openlonet

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import de.deftk.lonet.api.LoNetClient
import de.deftk.lonet.api.auth.Credentials
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.lonet.api.implementation.User
import de.deftk.lonet.api.model.IRequestContext
import de.deftk.lonet.api.request.handler.AutoLoginRequestHandler
import de.deftk.openlonet.activities.LoginActivity
import de.deftk.openlonet.fragments.dialog.ChooseAccountDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

object AuthStore {

    private const val LOG_TAG = "AuthStore"
    const val ACCOUNT_TYPE = "OpenLoNet/lo-net2.de"
    const val EXTRA_TOKEN_TYPE = "de.deftk.openlonet.auth.extra_token_type"
    private const val LAST_LOGIN_PREFERENCE = "last_login"

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
        context.setRequestHandler(AutoLoginRequestHandler(object : AutoLoginRequestHandler.LoginHandler<ApiContext> {
            override fun getCredentials(): Credentials {
                if (currentApiToken != null) {
                    return Credentials.fromToken(getApiUser().login, currentApiToken!!)
                }
                error("Can't provide credentials")
            }

            override fun onLogin(context: ApiContext) {
                setApiContext(context)
            }
        }, ApiContext::class.java))
    }

    fun isUserLoggedIn(): Boolean {
        return apiContext != null
    }

    fun doLoginProcedure(context: Context, fragmentManager: FragmentManager, allowNewLogin: Boolean, onSuccess: suspend () -> Unit, onFailure: suspend () -> Unit) {
        fun doLogin(account: Account, accountManager: AccountManager) {
            CoroutineScope(Dispatchers.IO).launch {
                if (performLogin(account, accountManager, context)) {
                    onSuccess()
                } else {
                    onFailure()
                }
            }
        }

        val accountManager = AccountManager.get(context)
        val accounts = findAccounts(accountManager, context)
        when {
            accounts.size == 1 -> doLogin(accounts[0], accountManager)
            accounts.size > 1 -> {
                ChooseAccountDialogFragment(accounts, object: ChooseAccountDialogFragment.AccountSelectListener {
                    override fun onAccountSelected(account: Account) {
                        doLogin(account, accountManager)
                    }
                }).show(fragmentManager, "ChooseAccount")
            }
            else -> {
                if (allowNewLogin) {
                    // add new account or simply login without adding an account
                    val intent = Intent(context, LoginActivity::class.java)
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, R.string.no_user, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun findAccounts(accountManager: AccountManager, context: Context): Array<Account> {
        val lastLogin = PreferenceManager.getDefaultSharedPreferences(context).getString(LAST_LOGIN_PREFERENCE, null)
        val allAccounts = accountManager.getAccountsByType(ACCOUNT_TYPE)
        if (lastLogin == null)
            return allAccounts
        if (allAccounts.any { it.name == lastLogin })
            return allAccounts.filter { it.name == lastLogin }.toTypedArray()
        return allAccounts
    }

    /**
     * @return login successful or not
     */
    suspend fun performLogin(account: Account, accountManager: AccountManager, context: Context): Boolean {
        try {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(LAST_LOGIN_PREFERENCE, account.name)
                .apply()
            currentAccount = account
            currentApiToken = accountManager.blockingGetAuthToken(currentAccount, ACCOUNT_TYPE, true)
            setApiContext(LoNetClient.loginToken(account.name, currentApiToken ?: error("No token provided!"), false))
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
        return false
    }

}