package de.deftk.openlonet.activities.feature.mail

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.databinding.ActivityWriteMailBinding
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

    private lateinit var binding: ActivityWriteMailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteMailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.write_mail)

        binding.mailToAddress.setText(intent.getStringExtra(Intent.EXTRA_EMAIL) ?: "")
        binding.mailToAddressCc.setText(intent.getStringExtra(Intent.EXTRA_CC) ?: "")
        binding.mailToAddressBcc.setText(intent.getStringExtra(Intent.EXTRA_BCC) ?: "")
        binding.mailSubject.setText(intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "")
        binding.mailMessage.setText(intent.getStringExtra(Intent.EXTRA_TEXT) ?: "")

        if (intent.data != null) {
            val uri = intent.data!!
            binding.mailToAddress.setText(uri.schemeSpecificPart ?: "")
        }

        binding.fabSendMail.setOnClickListener {
            val subject = binding.mailSubject.text.toString()
            val message = binding.mailMessage.text.toString()
            val to = binding.mailToAddress.text.toString()
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
            binding.progressSendMail.visibility = View.VISIBLE
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
                        binding.progressSendMail.visibility = View.GONE
                        setResult(RESULT_CODE_CANCEL)
                        finish()
                    }
                }
            }
        }
    }

    private suspend fun sendEmail() {
        val subject = binding.mailSubject.text.toString()
        val message = binding.mailMessage.text.toString()
        val to = binding.mailToAddress.text.toString()
        val toCC = binding.mailToAddressCc.text.toString()
        val toBCC = binding.mailToAddressBcc.text.toString()
        AuthStore.getApiUser().sendEmail(
            to = to,
            subject = subject,
            plainBody = message,
            bcc = toBCC.nullIfEmpty(),
            cc = toCC.nullIfEmpty(),
            context = AuthStore.getUserContext()
        )
        withContext(Dispatchers.Main) {
            binding.progressSendMail.visibility = View.GONE
            setResult(RESULT_CODE_MAIL)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //FIXME use API features to handle login
        /*when (requestCode) {
            AuthStore.REQUEST_LOGIN -> {
                if (resultCode == RESULT_OK) {
                    CoroutineScope(Dispatchers.IO).launch {
                        sendEmail()
                    }
                }
            }
            else ->*/ super.onActivityResult(requestCode, resultCode, data)
        //}
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
