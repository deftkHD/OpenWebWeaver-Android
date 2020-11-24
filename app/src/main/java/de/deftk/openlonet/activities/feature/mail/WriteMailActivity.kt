package de.deftk.openlonet.activities.feature.mail

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import kotlinx.android.synthetic.main.activity_write_mail.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class WriteMailActivity : AppCompatActivity() {

    companion object {
        const val RESULT_CODE_MAIL = 1
        const val RESULT_CODE_CANCEL = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_mail)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.write_mail)

        mail_to_address.setText(intent.getStringExtra(Intent.EXTRA_EMAIL) ?: "")
        mail_to_address_cc.setText(intent.getStringExtra(Intent.EXTRA_CC) ?: "")
        mail_to_address_bcc.setText(intent.getStringExtra(Intent.EXTRA_BCC) ?: "")
        mail_subject.setText(intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "")
        mail_message.setText(intent.getStringExtra(Intent.EXTRA_TEXT) ?: "")

        if (intent.data != null) {
            val uri = intent.data!!
            mail_to_address.setText(uri.schemeSpecificPart ?: "")
        }

        fab_send_mail.setOnClickListener {
            val subject = mail_subject.text.toString()
            val message = mail_message.text.toString()
            val to = mail_to_address.text.toString()
            if (subject.isEmpty()) {
                Toast.makeText(this, R.string.mail_no_subject, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (message.isEmpty()) {
                Toast.makeText(this, R.string.mail_no_message, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (to.isEmpty()) {
                Toast.makeText(this, R.string.mail_no_to, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            progress_send_mail.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (!AuthStore.isUserLoggedIn()) {
                        if (AuthStore.performLogin(this@WriteMailActivity)) {
                            sendEmail()
                        }
                    } else {
                        sendEmail()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@WriteMailActivity, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
                        progress_send_mail.visibility = View.GONE
                        setResult(RESULT_CODE_CANCEL)
                        finish()
                    }
                }
            }
        }
    }

    private suspend fun sendEmail() {
        val subject = mail_subject.text.toString()
        val message = mail_message.text.toString()
        val to = mail_to_address.text.toString()
        val toCC = mail_to_address_cc.text.toString()
        val toBCC = mail_to_address_bcc.text.toString()
        AuthStore.getAppUser().sendEmail(to, subject, message, null, toBCC.nullIfEmpty(), toCC.nullIfEmpty())
        withContext(Dispatchers.Main) {
            progress_send_mail.visibility = View.GONE
            setResult(RESULT_CODE_MAIL)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            AuthStore.REQUEST_LOGIN -> {
                if (resultCode == RESULT_OK) {
                    CoroutineScope(Dispatchers.IO).launch {
                        sendEmail()
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun String.nullIfEmpty(): String? {
        return if (this.isBlank()) null else this
    }

}
