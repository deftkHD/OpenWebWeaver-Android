package de.deftk.lonet.mobile.activities.feature.mail

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.mailbox.EmailFolder
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
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

        const val REQUEST_CODE_WRITE_MAIL = 1
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

        if (AuthStore.appUser.effectiveRights.contains(Permission.MAILBOX_ADMIN)) {
            fab_mail_add.visibility = View.VISIBLE
            fab_mail_add.setOnClickListener {
                val intent = Intent(this, WriteMailActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_WRITE_MAIL)
            }
        }

        reloadMails()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_WRITE_MAIL) {
            if (resultCode == WriteMailActivity.RESULT_CODE_MAIL) {
                progress_mail.visibility = View.VISIBLE
                reloadMails()
            }
        } else super.onActivityResult(requestCode, resultCode, data)
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