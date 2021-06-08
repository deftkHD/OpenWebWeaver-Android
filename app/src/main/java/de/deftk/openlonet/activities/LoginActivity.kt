package de.deftk.openlonet.activities

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import de.deftk.lonet.api.LoNetClient
import de.deftk.lonet.api.auth.Credentials
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.auth.LoNetAuthenticator
import de.deftk.openlonet.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Deprecated("use fragment architecture")
class LoginActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LOGIN = "de.deftk.openlonet.login.extra_login"
        const val EXTRA_REMEMBER = "de.deftk.openlonet.login.extra_remember"
        const val EXTRA_REFRESH_ACCOUNT = "de.deftk.openlonet.login.extra_do_refresh"
        private const val LOG_TAG = "LoginActivity"

        private const val REQUEST_TOKEN_LOGIN = 1
    }

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        if (intent.hasExtra(EXTRA_LOGIN)) {
            binding.txtEmail.setText(intent.getStringExtra(EXTRA_LOGIN), TextView.BufferType.EDITABLE)
        }
        val refreshAccount = intent.getStringExtra(EXTRA_REFRESH_ACCOUNT)
        binding.chbStayLoggedIn.isChecked = intent.getBooleanExtra(EXTRA_REMEMBER, false) || refreshAccount != null
        binding.chbStayLoggedIn.isVisible = refreshAccount == null

        binding.tokenLogin.setOnClickListener {
            val intent = Intent(this, TokenLoginActivity::class.java)
            if (binding.txtEmail.text.isNotEmpty())
                intent.putExtra(EXTRA_LOGIN, binding.txtEmail.text.toString())
            startActivityForResult(intent, REQUEST_TOKEN_LOGIN)
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.txtEmail.text.toString()
            val password = binding.txtPassword.text.toString()
            val stayLoggedIn = binding.chbStayLoggedIn.isChecked
            if (isEmailValid(username) && isPasswordValid(password)) {
                Log.i(LOG_TAG, "Calling login task")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (stayLoggedIn) {
                            val accountManager = AccountManager.get(this@LoginActivity)
                            if (refreshAccount != null) {
                                val account = accountManager.getAccountsByType(LoNetAuthenticator.ACCOUNT_TYPE).firstOrNull { it.name == refreshAccount } ?: error("Unknown account")
                                val (context, token) = LoNetClient.loginCreateToken(username, password, "OpenLoNet", "${Build.BRAND} ${Build.MODEL}")
                                accountManager.setPassword(account, token)
                                AuthStore.setApiContext(context)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    accountManager.notifyAccountAuthenticated(account)
                                }
                            } else {
                                val account = Account(username, LoNetAuthenticator.ACCOUNT_TYPE)
                                val (context, token) = LoNetClient.loginCreateToken(username, password, "OpenLoNet", "${Build.BRAND} ${Build.MODEL}")
                                accountManager.addAccountExplicitly(account, token, null)
                                AuthStore.setApiContext(context)
                            }
                        } else {
                            AuthStore.setApiContext(LoNetClient.login(Credentials.fromPassword(username, password), ApiContext::class.java))
                        }
                        withContext(Dispatchers.Main) {
                            Log.i(LOG_TAG, "Got login result")
                            binding.pgbLogin.visibility = ProgressBar.INVISIBLE
                            binding.btnLogin.isEnabled = true
                            Log.i(LOG_TAG, "Login succeeded")
                            Toast.makeText(this@LoginActivity, "${getString(R.string.login_success)}!", Toast.LENGTH_SHORT).show()
                            //startActivity(Intent(this@LoginActivity, StartActivity::class.java))
                            finish()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            binding.pgbLogin.visibility = ProgressBar.INVISIBLE
                            binding.btnLogin.isEnabled = true
                            AuthStore.handleLoginException(e, this@LoginActivity, false, username)
                        }
                    }
                }
                binding.pgbLogin.visibility = ProgressBar.VISIBLE
                binding.btnLogin.isEnabled = false
            } else {
                Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@") && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotBlank()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TOKEN_LOGIN) {
            if (resultCode == RESULT_OK) {
                //startActivity(Intent(this@LoginActivity, StartActivity::class.java))
                finish()
            }
        } else super.onActivityResult(requestCode, resultCode, data)
    }

}
