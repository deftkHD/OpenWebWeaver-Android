package de.deftk.lonet.mobile.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.tasks.LoginTask
import kotlinx.android.synthetic.main.activity_login.*
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

class LoginActivity : AppCompatActivity(), LoginTask.ILoginCallback {

    companion object {
        const val EXTRA_LOGIN = "extra_login"
        private const val LOG_TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportActionBar?.hide()

        if (intent != null) {
            if (intent.hasExtra(EXTRA_LOGIN)) {
                txtEmail.setText(intent.getStringExtra(EXTRA_LOGIN), TextView.BufferType.EDITABLE)
            }
        }

        btnLogin.setOnClickListener {
            if (isEmailValid(txtEmail.text.toString()) && isPasswordValid(txtPassword.text.toString())) {
                Log.i(LOG_TAG, "Calling login task")
                LoginTask(this).execute(
                    txtEmail.text.toString(),
                    txtPassword.text.toString(),
                    if (chbStayLoggedIn.isChecked) LoginTask.LoginMethod.PASSWORD_CREATE_TRUST else LoginTask.LoginMethod.PASSWORD
                )
                pgbLogin.visibility = ProgressBar.VISIBLE
                btnLogin.isEnabled = false
            } else {
                Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@") && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotBlank()
    }

    override fun onLoginResult(result: LoginTask.LoginResult) {
        Log.i(LOG_TAG, "Got login result")
        pgbLogin?.visibility = ProgressBar.INVISIBLE
        btnLogin?.isEnabled = true
        if (result.failed()) {
            Log.e(LOG_TAG, "Login failed")
            if (result.exception is IOException) {
                when (result.exception) {
                    is UnknownHostException ->
                        Toast.makeText(this, getString(R.string.request_failed_connection), Toast.LENGTH_LONG).show()
                    is TimeoutException ->
                        Toast.makeText(this, getString(R.string.request_failed_timeout), Toast.LENGTH_LONG).show()
                    else ->
                        Toast.makeText(this, String.format(getString(R.string.request_failed_other), result.exception.message), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "${getString(R.string.login_failed)}: ${result.exception?.message ?: result.exception ?: "No details"}", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.i(LOG_TAG, "Login succeeded")
            Toast.makeText(this, "${getString(R.string.login_success)}!", Toast.LENGTH_SHORT).show()
            AuthStore.appUser = result.user ?: error("How should I understand this?")
            if (result.saveKey) {
                AuthStore.saveUsername(result.user.getLogin(), this)
                AuthStore.saveToken(result.user.authKey, this)
            }

            Log.i(LOG_TAG, "Starting MainActivity")
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

}
