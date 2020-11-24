package de.deftk.openlonet.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.BuildConfig
import de.deftk.openlonet.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                if (AuthStore.performLogin(this@MainActivity)) {
                    val intent = Intent(this@MainActivity, StartActivity::class.java)
                    withContext(Dispatchers.Main) {
                        startActivity(intent)
                    }
                }
            }
        }
        Log.i(LOG_TAG, "Created MainActivity")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.i(LOG_TAG, "Permission result")
        when (requestCode) {
            0 -> {
                if (grantResults.getOrNull(0) != PackageManager.PERMISSION_GRANTED) {
                    Log.i(LOG_TAG, "Permission not granted")
                    finish()
                } else {
                    Log.i(LOG_TAG, "Permission granted")
                    CoroutineScope(Dispatchers.IO).launch {
                        AuthStore.performLogin(this@MainActivity)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            AuthStore.REQUEST_LOGIN -> {
                if (resultCode == RESULT_OK) {
                    val intent = Intent(this@MainActivity, StartActivity::class.java)
                    startActivity(intent)
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

}
