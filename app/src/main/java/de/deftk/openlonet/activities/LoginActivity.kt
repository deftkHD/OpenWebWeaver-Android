package de.deftk.openlonet.activities

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.LoNetClient
import de.deftk.lonet.api.auth.Credentials
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

class LoginActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LOGIN = "de.deftk.openlonet.login.extra_login"
        const val EXTRA_REMEMBER = "de.deftk.openlonet.login.extra_remember"
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
        binding.chbStayLoggedIn.isChecked = intent.getBooleanExtra(EXTRA_REMEMBER, false)

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
                            val account = Account(username, AuthStore.ACCOUNT_TYPE)
                            accountManager.addAccountExplicitly(account, password, null)
                            val token = accountManager.blockingGetAuthToken(account, AuthStore.ACCOUNT_TYPE, true)
                            accountManager.setAuthToken(account, AuthStore.EXTRA_TOKEN_TYPE, token)
                        } else {
                            AuthStore.setApiContext(LoNetClient.login(Credentials.fromPassword(username, password), ApiContext::class.java))
                        }
                        withContext(Dispatchers.Main) {
                            Log.i(LOG_TAG, "Got login result")
                            binding.pgbLogin.visibility = ProgressBar.INVISIBLE
                            binding.btnLogin.isEnabled = true
                            Log.i(LOG_TAG, "Login succeeded")
                            Toast.makeText(this@LoginActivity, "${getString(R.string.login_success)}!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, StartActivity::class.java))
                            finish()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            e.printStackTrace()
                            Log.i(LOG_TAG, "Got login result")
                            binding.pgbLogin.visibility = ProgressBar.INVISIBLE
                            binding.btnLogin.isEnabled = true
                            Log.e(LOG_TAG, "Login failed")
                            if (e is IOException) {
                                when (e) {
                                    is UnknownHostException ->
                                        Toast.makeText(this@LoginActivity, getString(R.string.request_failed_connection), Toast.LENGTH_LONG).show()
                                    is TimeoutException ->
                                        Toast.makeText(this@LoginActivity, getString(R.string.request_failed_timeout), Toast.LENGTH_LONG).show()
                                    else ->
                                        Toast.makeText(this@LoginActivity, String.format(getString(R.string.request_failed_other), e.message), Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(this@LoginActivity, "${getString(R.string.login_failed)}: ${e.message ?: e}", Toast.LENGTH_LONG).show()
                            }
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
                startActivity(Intent(this@LoginActivity, StartActivity::class.java))
                finish()
            }
        } else super.onActivityResult(requestCode, resultCode, data)
    }

}
