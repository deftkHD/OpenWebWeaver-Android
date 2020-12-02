package de.deftk.openlonet.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import de.deftk.lonet.api.LoNet
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_token_login.*
import kotlinx.android.synthetic.main.activity_token_login.btnLogin
import kotlinx.android.synthetic.main.activity_token_login.pgbLogin
import kotlinx.android.synthetic.main.activity_token_login.txtEmail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

class TokenLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_token_login)

        if (intent.hasExtra(LoginActivity.EXTRA_LOGIN)) {
            txtEmail.setText(intent.getStringExtra(LoginActivity.EXTRA_LOGIN), TextView.BufferType.EDITABLE)
        }

        btnLogin.setOnClickListener {
            val email = txtEmail.text.toString()
            val token = txtToken.text.toString()
            val rememberToken = chbRememberToken.isChecked
            if (isEmailValid(email) && isTokenValid(token)) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AuthStore.setAppUser(LoNet.loginToken(email, token))
                        if (rememberToken) {
                            AuthStore.saveUsername(email, this@TokenLoginActivity)
                            AuthStore.saveToken(token, this@TokenLoginActivity)
                        }
                        withContext(Dispatchers.Main) {
                            pgbLogin?.visibility = ProgressBar.INVISIBLE
                            btnLogin?.isEnabled = true
                            Toast.makeText(this@TokenLoginActivity, "${getString(R.string.login_success)}!", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            e.printStackTrace()
                            pgbLogin?.visibility = ProgressBar.INVISIBLE
                            btnLogin?.isEnabled = true
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
                pgbLogin.visibility = ProgressBar.VISIBLE
                btnLogin.isEnabled = false
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