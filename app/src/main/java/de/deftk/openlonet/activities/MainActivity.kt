package de.deftk.openlonet.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

        CoroutineScope(Dispatchers.IO).launch {
            if (AuthStore.performLogin(this@MainActivity)) {
                val intent = Intent(this@MainActivity, StartActivity::class.java)
                withContext(Dispatchers.Main) {
                    startActivity(intent)
                }
            }
        }
        Log.i(LOG_TAG, "Created MainActivity")
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
