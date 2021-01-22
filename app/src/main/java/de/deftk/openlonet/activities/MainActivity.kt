package de.deftk.openlonet.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.BuildConfig
import de.deftk.openlonet.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LOGOUT = "de.deftk.openlonet.main.logout"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        binding.lblVersion.append(" ${BuildConfig.VERSION_NAME}")

        CoroutineScope(Dispatchers.IO).launch {
            if (AuthStore.performLogin(this@MainActivity)) {
                val intent = Intent(this@MainActivity, StartActivity::class.java)
                if (getIntent().getBooleanExtra(EXTRA_LOGOUT, false))
                    intent.putExtra(StartActivity.EXTRA_LOGOUT, true)
                withContext(Dispatchers.Main) {
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

}
