package de.deftk.lonet.mobile.activities.feature

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.feature.mailbox.Email
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import kotlinx.android.synthetic.main.activity_read_mail.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat

class ReadMailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MAIL = "de.deftk.lonet.mobile.mail.mail_extra"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_mail)

        // back button in toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.read_mail)

        val mail = intent.getSerializableExtra(EXTRA_MAIL) as? Email

        if (mail != null) {
            mail_subject?.text = mail.subject
            mail_author?.text = mail.from?.joinToString { it.name } ?: AuthStore.appUser.getName()
            mail_author_address?.text = mail.from?.joinToString { it.address } ?: AuthStore.appUser.getLogin()
            mail_date?.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(mail.date)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val content = mail.read()
                    withContext(Dispatchers.Main) {
                        progress_read_mail?.visibility = ProgressBar.INVISIBLE
                        mail_message?.text = content.text ?: content.plainBody
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progress_read_mail?.visibility = ProgressBar.INVISIBLE
                        Toast.makeText(this@ReadMailActivity, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
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

}
