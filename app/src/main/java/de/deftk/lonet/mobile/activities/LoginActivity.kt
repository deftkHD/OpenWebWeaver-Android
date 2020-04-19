package de.deftk.lonet.mobile.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.tasks.LoginTask

class LoginActivity : AppCompatActivity(), LoginTask.ILoginCallback {

    companion object {
        const val EXTRA_LOGIN = "extra_login"
    }

    private val progressBar by lazy { findViewById<ProgressBar>(R.id.pgbLogin) }
    private val txtEmail by lazy { findViewById<EditText>(R.id.txtEmail) }
    private val txtPassword by lazy { findViewById<EditText> (R.id.txtPassword) }
    private val chbStayLoggedIn by lazy { findViewById<CheckBox>(R.id.chbStayLoggedIn) }
    private val btnLogin by lazy { findViewById<Button>(R.id.btnLogin) }

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
                LoginTask(this).execute(
                    txtEmail.text.toString(),
                    txtPassword.text.toString(),
                    if (chbStayLoggedIn.isChecked) LoginTask.LoginMethod.PASSWORD_CREATE_TRUST else LoginTask.LoginMethod.PASSWORD
                )
                progressBar.visibility = ProgressBar.VISIBLE
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
        progressBar.visibility = ProgressBar.INVISIBLE
        btnLogin.isEnabled = true
        if (result.failed()) {
            Toast.makeText(this, "${getString(R.string.login_failed)}: ${result.exception?.message ?: result.exception ?: "No details"}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "${getString(R.string.login_success)}!", Toast.LENGTH_SHORT).show()
            AuthStore.appUser = result.user ?: error("How should I understand this?")
            if (result.saveKey) {
                AuthStore.saveUsername(result.user.login, this)
                AuthStore.saveToken(result.user.authKey, this)
            }

            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

}
