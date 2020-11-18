package de.deftk.lonet.mobile.activities.feature.mail

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import de.deftk.lonet.api.model.Group
import de.deftk.lonet.api.model.feature.mailbox.Email
import de.deftk.lonet.api.model.feature.mailbox.EmailFolder
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.activities.feature.MembersActivity
import de.deftk.lonet.mobile.adapter.MailAdapter
import de.deftk.lonet.mobile.adapter.MailFolderAdapter
import kotlinx.android.synthetic.main.activity_mails.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

class MailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FOLDER = "de.deftk.lonet.mobile.mail.extra_folder"
    }

    private lateinit var folder: EmailFolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mails)

        val extraFolder = intent.getSerializableExtra(EXTRA_FOLDER) as? EmailFolder?
        if (extraFolder != null) {
            folder = extraFolder
        } else {
            finish()
            return
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = MailFolderAdapter.getDefaultFolderTranslation(this, folder)

        mail_swipe_refresh.setOnRefreshListener {
            reloadMails()
        }
        mail_list.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, ReadMailActivity::class.java)
            intent.putExtra(ReadMailActivity.EXTRA_MAIL, mail_list.getItemAtPosition(position) as Serializable)
            startActivity(intent)
        }

        reloadMails()
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun reloadMails() {
        mail_list.adapter = null
        CoroutineScope(Dispatchers.IO).launch {
            loadMails()
        }
    }

    private suspend fun loadMails() {
        try {
            val emails = folder.getEmails()
            withContext(Dispatchers.Main) {
                mail_list?.adapter = MailAdapter(this@MailsActivity, emails)
                mail_empty?.isVisible = emails.isEmpty()
                progress_mail?.visibility = ProgressBar.INVISIBLE
                mail_swipe_refresh?.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                progress_mail?.visibility = ProgressBar.INVISIBLE
                mail_swipe_refresh?.isRefreshing = false
                Toast.makeText(this@MailsActivity, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
            }
        }
    }

}