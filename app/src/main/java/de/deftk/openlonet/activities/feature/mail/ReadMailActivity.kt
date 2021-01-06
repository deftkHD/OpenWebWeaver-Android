package de.deftk.openlonet.activities.feature.mail

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.implementation.feature.mailbox.Email
import de.deftk.lonet.api.implementation.feature.mailbox.EmailFolder
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.databinding.ActivityReadMailBinding
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.getJsonExtra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat

class ReadMailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MAIL = "de.deftk.openlonet.mail.mail_extra"
        const val EXTRA_FOLDER = "de.deftk.openlonet.mail.folder_extra"
    }

    private lateinit var binding: ActivityReadMailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadMailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // back button in toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.read_mail)

        if (intent.hasExtra(EXTRA_MAIL) && intent.hasExtra(EXTRA_FOLDER)) {
            val mail = intent.getJsonExtra<Email>(EXTRA_MAIL)!!
            val folder = intent.getJsonExtra<EmailFolder>(EXTRA_FOLDER)!!

            binding.mailSubject.text = mail.getSubject()
            binding.mailAuthor.text = mail.getFrom()?.joinToString { it.name } ?: AuthStore.getApiUser().name
            binding.mailAuthorAddress.text = mail.getFrom()?.joinToString { it.address } ?: AuthStore.getApiUser().login
            binding.mailDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(mail.getDate())
            binding.mailMessage.transformationMethod = CustomTabTransformationMethod(binding.mailMessage.autoLinkMask)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    mail.read(folder, false, AuthStore.getUserContext())
                    withContext(Dispatchers.Main) {
                        binding.progressReadMail.visibility = ProgressBar.INVISIBLE
                        binding.mailMessage.text = TextUtils.parseMultipleQuotes(TextUtils.parseHtml(mail.getText() ?: mail.getPlainBody()))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        binding.progressReadMail.visibility = ProgressBar.INVISIBLE
                        Toast.makeText(this@ReadMailActivity, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            finish()
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}
