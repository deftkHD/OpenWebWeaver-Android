package de.deftk.openlonet.activities

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
import de.deftk.openlonet.databinding.ActivityTokenLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

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
            val email = binding.txtEmail.text.toString()
            val token = binding.txtToken.text.toString()
            val rememberToken = binding.chbRememberToken.isChecked
            if (isEmailValid(email) && isTokenValid(token)) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AuthStore.setApiContext(LoNetClient.loginToken(email, token, false, ApiContext::class.java))
                        if (rememberToken) {
                            AuthStore.saveUsername(email, this@TokenLoginActivity)
                            AuthStore.saveToken(token, this@TokenLoginActivity)
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
                            e.printStackTrace()
                            binding.pgbLogin.visibility = ProgressBar.INVISIBLE
                            binding.btnLogin.isEnabled = true
                            setResult(RESULT_CANCELED)
                            if (e is IOException) {
                                when (e) {
                                    is UnknownHostException ->
                                        Toast.makeText(this@TokenLoginActivity, getString(R.string.request_failed_connection), Toast.LENGTH_LONG).show()
                                    is TimeoutException ->
                                        Toast.makeText(this@TokenLoginActivity, getString(R.string.request_failed_timeout), Toast.LENGTH_LONG).show()
                                    else ->
                                        Toast.makeText(this@TokenLoginActivity, String.format(getString(R.string.request_failed_other), e.message), Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(this@TokenLoginActivity, "${getString(R.string.login_failed)}: ${e.message ?: e}", Toast.LENGTH_LONG).show()
                            }
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