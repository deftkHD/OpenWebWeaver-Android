package de.deftk.lonet.mobile.activities.feature

import android.os.AsyncTask
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.feature.mailbox.Email
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import kotlinx.android.synthetic.main.activity_read_mail.*
import java.text.DateFormat

class ReadMailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MAIL = "de.deftk.lonet.mobile.mail.mail_extra"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_mail)

        // back button in toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.read_mail)

        val mail = intent.getSerializableExtra(EXTRA_MAIL) as? Email

        if (mail != null) {
            mail_subject?.text = mail.subject
            mail_author?.text = mail.from?.joinToString { it.name } ?: AuthStore.appUser.name ?: ""
            mail_author_address?.text = mail.from?.joinToString { it.address } ?: AuthStore.appUser.login
            mail_date?.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(mail.date)
            MailContentLoader().execute(mail)
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private inner class MailContentLoader: AsyncTask<Email, Void, Email.EmailContent?>() {

        override fun doInBackground(vararg params: Email): Email.EmailContent? {
            return try {
                params[0].read(AuthStore.appUser)
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: Email.EmailContent?) {
            if (result != null) {
                mail_message?.text = result.text ?: result.plainBody
                progress_read_mail?.visibility = ProgressBar.INVISIBLE
                //TODO refresh fragment (so subject is not bold anymore)
            } else {
                Toast.makeText(baseContext, getString(R.string.request_failed_other).format("No details"), Toast.LENGTH_LONG).show()
            }

        }

    }

}
