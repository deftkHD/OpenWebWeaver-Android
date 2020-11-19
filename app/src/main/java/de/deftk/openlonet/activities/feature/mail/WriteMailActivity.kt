package de.deftk.openlonet.activities.feature.mail

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

        fab_send_mail.setOnClickListener {
            val subject = mail_subject.text.toString()
            val message = mail_message.text.toString()
            val to = mail_to_address.text.toString()
            val toCC = mail_to_address_cc.text.toString()
            val toBCC = mail_to_address_bcc.text.toString()
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
                    AuthStore.appUser.sendEmail(to, subject, message, null, toBCC.nullIfEmpty(), toCC.nullIfEmpty())
                    withContext(Dispatchers.Main) {
                        progress_send_mail.visibility = View.GONE
                        setResult(RESULT_CODE_MAIL)
                        finish()
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

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun String.nullIfEmpty(): String? {
        return if (this.isBlank()) null else this
    }

}
