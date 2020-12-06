package de.deftk.openlonet.activities.feature.mail

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.feature.mailbox.Email
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.databinding.ActivityReadMailBinding
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat

class ReadMailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MAIL = "de.deftk.openlonet.mail.mail_extra"
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

        val mail = intent.getSerializableExtra(EXTRA_MAIL) as? Email

        if (mail != null) {
            binding.mailSubject.text = mail.subject
            binding.mailAuthor.text = mail.from?.joinToString { it.name } ?: AuthStore.getAppUser().getName()
            binding.mailAuthorAddress.text = mail.from?.joinToString { it.address } ?: AuthStore.getAppUser().getLogin()
            binding.mailDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(mail.date)
            binding.mailMessage.transformationMethod = CustomTabTransformationMethod(binding.mailMessage.autoLinkMask)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val content = mail.read()
                    withContext(Dispatchers.Main) {
                        binding.progressReadMail.visibility = ProgressBar.INVISIBLE
                        binding.mailMessage.text = TextUtils.parseMultipleQuotes(TextUtils.parseHtml(content.text ?: content.plainBody))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        binding.progressReadMail.visibility = ProgressBar.INVISIBLE
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
