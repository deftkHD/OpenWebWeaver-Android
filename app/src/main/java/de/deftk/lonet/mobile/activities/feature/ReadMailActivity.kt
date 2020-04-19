package de.deftk.lonet.mobile.activities.feature

import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ProgressBar
import de.deftk.lonet.api.model.feature.mailbox.Email
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import kotlinx.android.synthetic.main.activity_read_mail.*
import java.text.DateFormat

class ReadMailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FOLDER_ID = "de.deftk.lonet.mobile.mail.extra_folder_id"
        const val EXTRA_MAIL_ID = "de.deftk.lonet.mobile.mail.extra_mail_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_mail)

        // back button in toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val mailId = intent.getIntExtra(EXTRA_MAIL_ID, -1)
        val folderId = intent.getStringExtra(EXTRA_FOLDER_ID)
        val folder = AuthStore.appUser.getEmailFolders().first { it.id == folderId }
        val mail = folder.getEmails(AuthStore.appUser.sessionId).first { it.id == mailId }

        mail_subject?.text = mail.subject
        mail_author?.text = mail.from?.joinToString { it.name } ?: AuthStore.appUser.name ?: ""
        mail_author_address?.text = mail.from?.joinToString { it.address } ?: AuthStore.appUser.login
        mail_date?.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(mail.date)
        MailContentLoader().execute(mail)
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private inner class MailContentLoader: AsyncTask<Email, Void, Email.EmailContent>() {

        override fun doInBackground(vararg params: Email): Email.EmailContent {
            return params[0].read(AuthStore.appUser.sessionId)
        }

        override fun onPostExecute(result: Email.EmailContent) {
            mail_message?.text = result.text ?: result.plainBody
            progress_read_mail?.visibility = ProgressBar.INVISIBLE
            //TODO refresh fragment (so subject is not bold anymore)
        }

    }

}
