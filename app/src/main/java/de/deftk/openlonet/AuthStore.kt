package de.deftk.openlonet

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
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
import de.deftk.openlonet.auth.LoNetAuthenticator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

@Deprecated("use viewmodel instead")
object AuthStore {

    private const val LOG_TAG = "AuthStore"
    private const val LAST_LOGIN_PREFERENCE = "last_login"

    var currentAccount: Account? = null

    private var apiContext: ApiContext? = null
    private lateinit var accountManager: AccountManager

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
                if (currentAccount != null) {
                    return Credentials.fromToken(currentAccount!!.name, accountManager.getPassword(currentAccount))
                }
                error("Can't provide credentials")
            }

            override fun onLogin(context: ApiContext) {
                if (currentAccount != null) {
                    // make sure old session id is invalidated
                    accountManager.invalidateAuthToken(LoNetAuthenticator.EXTRA_TOKEN_TYPE, getApiContext().getSessionId())
                }
                setApiContext(context)
            }
        }, ApiContext::class.java))
    }

    fun isUserLoggedIn(): Boolean {
        return apiContext != null
    }

    fun doLoginProcedure(context: Context, fragmentManager: FragmentManager, allowNewLogin: Boolean, allowRefreshLogin: Boolean, priorisedLogin: String?, onSuccess: suspend () -> Unit, onFailure: suspend () -> Unit) {
        fun doLogin(account: Account, accountManager: AccountManager) {
            CoroutineScope(Dispatchers.IO).launch {
                if (performLogin(account, accountManager, allowRefreshLogin, context)) {
                    onSuccess()
                } else {
                    onFailure()
                }
            }
        }
        accountManager = AccountManager.get(context)
        val accounts = findAccounts(accountManager, priorisedLogin, context)
        when {
            accounts.size == 1 -> doLogin(accounts[0], accountManager)
            accounts.size > 1 -> {
                /*ChooseAccountDialogFragment(accounts, object: ChooseAccountDialogFragment.AccountSelectListener {
                    override fun onAccountSelected(account: Account) {
                        doLogin(account, accountManager)
                    }
                }).show(fragmentManager, "ChooseAccount")*/
            }
            else -> {
                if (allowNewLogin) {
                    // add new account or simply login without adding an account
                    /*val intent = Intent(context, LoginActivity::class.java)
                    context.startActivity(intent)*/
                } else {
                    Toast.makeText(context, R.string.no_user, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun findAccounts(accountManager: AccountManager, priorisedLogin: String?, context: Context): Array<Account> {
        val lastLogin = PreferenceManager.getDefaultSharedPreferences(context).getString(LAST_LOGIN_PREFERENCE, null)
        val allAccounts = accountManager.getAccountsByType(LoNetAuthenticator.ACCOUNT_TYPE)
        if (priorisedLogin != null && allAccounts.any { it.name == priorisedLogin })
            return allAccounts.filter { it.name == priorisedLogin }.toTypedArray()
        if (lastLogin == null)
            return allAccounts
        if (allAccounts.any { it.name == lastLogin })
            return allAccounts.filter { it.name == lastLogin }.toTypedArray()
        return allAccounts
    }

    /**
     * @return login successful or not
     */
    suspend fun performLogin(account: Account, accountManager: AccountManager, allowRefreshLogin: Boolean, context: Context): Boolean {
        try {
            setApiContext(LoNetClient.loginToken(account.name, accountManager.getPassword(account)))
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(LAST_LOGIN_PREFERENCE, account.name)
                .apply()
            currentAccount = account
            return true
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                handleLoginException(e, context, allowRefreshLogin, account.name)
            }
        }
        return false
    }

    fun handleLoginException(e: Exception, context: Context, allowRefreshLogin: Boolean, login: String) {
        Log.e(LOG_TAG, "Login failed")
        e.printStackTrace()
        if (e is IOException) {
            when (e) {
                is UnknownHostException -> Toast.makeText(context, context.getString(R.string.request_failed_connection), Toast.LENGTH_LONG).show()
                is TimeoutException -> Toast.makeText(context, context.getString(R.string.request_failed_timeout), Toast.LENGTH_LONG).show()
                else -> Toast.makeText(context, String.format(context.getString(R.string.request_failed_other), e.message), Toast.LENGTH_LONG).show()
            }
        } else {
            if (allowRefreshLogin) {
                Toast.makeText(context, context.getString(R.string.token_expired), Toast.LENGTH_LONG).show()
                /*val intent = Intent(context, LoginActivity::class.java)
                intent.putExtra(LoginActivity.EXTRA_REFRESH_ACCOUNT, login)
                intent.putExtra(LoginActivity.EXTRA_LOGIN, login)
                context.startActivity(intent)*/
            } else {
                Toast.makeText(context, "${context.getString(R.string.login_failed)}: ${e.message ?: e}", Toast.LENGTH_LONG).show()
            }
        }
    }

}