package de.deftk.openlonet.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.BuildConfig
import de.deftk.openlonet.R
import de.deftk.openlonet.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
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

        // beta disclaimer
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (!preferences.getBoolean("beta_disclaimer_shown", false)) {
            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.beta_disclaimer)
                .setMessage("")
                .setPositiveButton(R.string.next) { _, _ ->
                    preferences.edit().putBoolean("beta_disclaimer_shown", true).apply()
                    showPrivacyDisclaimer()
                }
                .setOnCancelListener {
                    finish()
                }
                .create()
            dialog.show()
            val textView = dialog.findViewById<TextView>(android.R.id.message)
            val str = SpannableString(getString(R.string.beta_disclaimer_text))
            Linkify.addLinks(str, Linkify.WEB_URLS)
            textView.text = str
            textView.movementMethod = LinkMovementMethod.getInstance()
        } else {
            showPrivacyDisclaimer()
        }
    }

    private fun showPrivacyDisclaimer() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        val country = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            resources.configuration.locales[0].isO3Country
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale.isO3Country
        }

        if (!preferences.getBoolean("privacy_disclaimer_shown", false) && country == "DEU") {
            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.privacy_title)
                .setMessage("")
                .setPositiveButton(R.string.agree) { _, _ ->
                    preferences.edit().putBoolean("privacy_disclaimer_shown", true).apply()
                    doLogin()
                }
                .setNegativeButton(R.string.decline) { _, _ ->
                    finish()
                }
                .setOnCancelListener {
                    finish()
                }
                .create()
            dialog.show()
            val textView = dialog.findViewById<TextView>(android.R.id.message)
            val str = SpannableString(getString(R.string.privacy_short))
            Linkify.addLinks(str, Linkify.WEB_URLS)
            textView.text = str
            textView.movementMethod = LinkMovementMethod.getInstance()
        } else {
            doLogin()
        }
    }

    private fun doLogin() {
        AuthStore.doLoginProcedure(this, supportFragmentManager, true, {
            // on success
            val intent = Intent(this, StartActivity::class.java)
            if (getIntent().getBooleanExtra(EXTRA_LOGOUT, false))
                intent.putExtra(StartActivity.EXTRA_LOGOUT, true)
            withContext(Dispatchers.Main) {
                startActivity(intent)
                finish()
            }
        }, {
            // on failure
            Toast.makeText(this, R.string.login_failed, Toast.LENGTH_LONG).show()
        })
    }

}
