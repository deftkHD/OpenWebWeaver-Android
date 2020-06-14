package de.deftk.lonet.mobile.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.BuildConfig
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.tasks.LoginTask
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

class MainActivity : AppCompatActivity(), LoginTask.ILoginCallback {

    companion object {
        private const val LOG_TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(LOG_TAG, "Creating MainActivity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        val lblVersion = findViewById<TextView>(R.id.lblVersion)
        lblVersion.append(" ${BuildConfig.VERSION_NAME}")

        // check if app has write & read permission
        Log.i(LOG_TAG, "Checking permissions")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i(LOG_TAG, "Requesting permissions")
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder(this)
                    .setMessage(R.string.files_permission_description)
                    .setPositiveButton(R.string.confirm) { _, _ ->
                        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                    }
                    .show()
            } else {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
            }
        } else {
            performLogin()
        }
        Log.i(LOG_TAG, "Created MainActivity")
    }

    override fun onLoginResult(result: LoginTask.LoginResult) {
        if (result.failed()) {
            Log.e(LOG_TAG, "Login failed")
            result.exception?.printStackTrace()
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
                getSharedPreferences(AuthStore.PREFERENCE_NAME, 0).edit().remove("token").apply()
                Toast.makeText(this, getString(R.string.token_expired), Toast.LENGTH_LONG).show()

                val intent = Intent(this, LoginActivity::class.java)
                if (AuthStore.getSavedUsername(this) != null) {
                    intent.putExtra(LoginActivity.EXTRA_LOGIN, AuthStore.getSavedUsername(this))
                }
                startActivity(intent)
                finish()
            }
        } else {
            AuthStore.appUser = result.user ?: error("Why is the user null?")

            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun performLogin() {
        if (AuthStore.getSavedToken(this) == null) {
            // create new account or simply login without adding an account
            val intent = Intent(this, LoginActivity::class.java)
            if (AuthStore.getSavedUsername(this) != null) {
                intent.putExtra(LoginActivity.EXTRA_LOGIN, AuthStore.getSavedUsername(this))
            }
            startActivity(intent)
            finish()
        } else {
            // use saved account
            LoginTask(this).execute(
                AuthStore.getSavedUsername(this),
                AuthStore.getSavedToken(this),
                LoginTask.LoginMethod.TRUST
            )
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.i(LOG_TAG, "Permission result")
        when (requestCode) {
            0 -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(LOG_TAG, "Permission not granted")
                    finish()
                } else {
                    Log.i(LOG_TAG, "Permission granted")
                    performLogin()
                }
            }
        }
    }

}
