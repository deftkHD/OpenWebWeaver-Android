package de.deftk.openlonet.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.LoNet
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

class LoginActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LOGIN = "extra_login"
        private const val LOG_TAG = "LoginActivity"

        private const val REQUEST_LOGIN_TOKEN = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportActionBar?.hide()

        if (intent.hasExtra(EXTRA_LOGIN)) {
            txtEmail.setText(intent.getStringExtra(EXTRA_LOGIN), TextView.BufferType.EDITABLE)
        }

        token_login.setOnClickListener {
            val intent = Intent(this, TokenLoginActivity::class.java)
            if (this.intent.hasExtra(EXTRA_LOGIN))
                intent.putExtra(EXTRA_LOGIN, this.intent.getStringExtra(EXTRA_LOGIN))
            startActivityForResult(intent, REQUEST_LOGIN_TOKEN)
        }

        btnLogin.setOnClickListener {
            val username = txtEmail.text.toString()
            val password = txtPassword.text.toString()
            val stayLoggedIn = chbStayLoggedIn.isChecked
            if (isEmailValid(username) && isPasswordValid(password)) {
                Log.i(LOG_TAG, "Calling login task")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (stayLoggedIn) {
                            AuthStore.setAppUser(LoNet.loginCreateTrust(username, password, "OpenLoNet", "${Build.BRAND} ${Build.MODEL}"))
                            AuthStore.saveUsername(AuthStore.getAppUser().getLogin(), this@LoginActivity)
                            AuthStore.saveToken(AuthStore.getAppUser().authKey, this@LoginActivity)
                        } else {
                            AuthStore.setAppUser(LoNet.login(username, password))
                        }
                        withContext(Dispatchers.Main) {
                            Log.i(LOG_TAG, "Got login result")
                            pgbLogin?.visibility = ProgressBar.INVISIBLE
                            btnLogin?.isEnabled = true
                            Log.i(LOG_TAG, "Login succeeded")
                            Toast.makeText(this@LoginActivity, "${getString(R.string.login_success)}!", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            e.printStackTrace()
                            Log.i(LOG_TAG, "Got login result")
                            pgbLogin?.visibility = ProgressBar.INVISIBLE
                            btnLogin?.isEnabled = true
                            Log.e(LOG_TAG, "Login failed")
                            setResult(RESULT_CANCELED)
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
                pgbLogin.visibility = ProgressBar.VISIBLE
                btnLogin.isEnabled = false
            } else {
                Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_LOGIN_TOKEN) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK)
                finish()
            }
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@") && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotBlank()
    }

}
