package de.deftk.openlonet.activities

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import android.util.Patterns
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.LoNetClient
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.auth.LoNetAuthenticator
import de.deftk.openlonet.databinding.ActivityTokenLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Deprecated("use fragment architecture")
class TokenLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTokenLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTokenLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra(LoginActivity.EXTRA_LOGIN)) {
            binding.txtEmail.setText(intent.getStringExtra(LoginActivity.EXTRA_LOGIN), TextView.BufferType.EDITABLE)
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.txtEmail.text.toString()
            val token = binding.txtToken.text.toString()
            val rememberToken = binding.chbRememberToken.isChecked
            if (isEmailValid(username) && isTokenValid(token)) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AuthStore.setApiContext(LoNetClient.loginToken(username, token, false, ApiContext::class.java))
                        if (rememberToken) {
                            val accountManager = AccountManager.get(this@TokenLoginActivity)
                            val account = Account(username, LoNetAuthenticator.ACCOUNT_TYPE)
                            accountManager.addAccountExplicitly(account, token, null)
                            AuthStore.setApiContext(LoNetClient.loginToken(username, token))
                        }
                        withContext(Dispatchers.Main) {
                            binding.pgbLogin.visibility = ProgressBar.INVISIBLE
                            binding.btnLogin.isEnabled = true
                            Toast.makeText(this@TokenLoginActivity, "${getString(R.string.login_success)}!", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            binding.pgbLogin.visibility = ProgressBar.INVISIBLE
                            binding.btnLogin.isEnabled = true
                            AuthStore.handleLoginException(e, this@TokenLoginActivity, false, username)
                            setResult(RESULT_CANCELED)
                        }
                    }
                }
                binding.pgbLogin.visibility = ProgressBar.VISIBLE
                binding.btnLogin.isEnabled = false
            } else {
                Toast.makeText(this, R.string.invalid_credentials, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@") && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isTokenValid(token: String): Boolean {
        return token.isNotBlank()
    }

}