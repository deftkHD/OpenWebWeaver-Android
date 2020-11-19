package de.deftk.openlonet.activities

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
import de.deftk.lonet.api.LoNet
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.BuildConfig
import de.deftk.openlonet.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

class MainActivity : AppCompatActivity() {

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
            CoroutineScope(Dispatchers.IO).launch {
                performLogin()
            }
        }
        Log.i(LOG_TAG, "Created MainActivity")
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
                    CoroutineScope(Dispatchers.IO).launch {
                        performLogin()
                    }
                }
            }
        }
    }

    private suspend fun performLogin() {
        if (AuthStore.getSavedToken(this) == null) {
            // create new account or simply login without adding an account
            val intent = Intent(this, LoginActivity::class.java)
            val savedUser = AuthStore.getSavedUsername(this)
            if (savedUser != null) {
                intent.putExtra(LoginActivity.EXTRA_LOGIN, savedUser)
            }
            withContext(Dispatchers.Main) {
                startActivity(intent)
                finish()
            }
        } else {
            val username = AuthStore.getSavedUsername(this)!!
            val token = AuthStore.getSavedToken(this)!!

            try {
                AuthStore.appUser = LoNet.loginToken(username, token)

                val intent = Intent(this@MainActivity, StartActivity::class.java)
                withContext(Dispatchers.Main) {
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Login failed")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (e is IOException) {
                        when (e) {
                            is UnknownHostException -> Toast.makeText(this@MainActivity, getString(R.string.request_failed_connection), Toast.LENGTH_LONG).show()
                            is TimeoutException -> Toast.makeText(this@MainActivity, getString(R.string.request_failed_timeout), Toast.LENGTH_LONG).show()
                            else -> Toast.makeText(this@MainActivity, String.format(getString(R.string.request_failed_other), e.message), Toast.LENGTH_LONG).show()
                        }
                    } else {
                        getSharedPreferences(AuthStore.PREFERENCE_NAME, 0).edit().remove("token").apply()
                        Toast.makeText(this@MainActivity, getString(R.string.token_expired), Toast.LENGTH_LONG).show()

                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        if (AuthStore.getSavedUsername(this@MainActivity) != null) { intent.putExtra(LoginActivity.EXTRA_LOGIN, AuthStore.getSavedUsername(this@MainActivity))
                        }
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }

}
