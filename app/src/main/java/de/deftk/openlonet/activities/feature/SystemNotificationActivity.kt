package de.deftk.openlonet.activities.feature

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.implementation.feature.systemnotification.SystemNotification
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.SystemNotificationAdapter
import de.deftk.openlonet.databinding.ActivitySystemNotificationBinding
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.getJsonExtra
import java.text.DateFormat

class SystemNotificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SYSTEM_NOTIFICATION = "de.deftk.openlonet.system_notifications.system_notification_extra"
    }

    private lateinit var binding: ActivitySystemNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySystemNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // back button in toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.see_system_notification)

        if (intent.hasExtra(EXTRA_SYSTEM_NOTIFICATION)) {
            val notification = intent.getJsonExtra<SystemNotification>(EXTRA_SYSTEM_NOTIFICATION)!!
            val type = notification.getMessageType()
            binding.systemNotificationTitle.text = getString(SystemNotificationAdapter.typeTranslationMap[type] ?: R.string.system_notification_type_unknown)

            binding.systemNotificationAuthor.text = notification.getMember().name
            binding.systemNotificationGroup.text = notification.getGroup().name
            binding.systemNotificationDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(notification.getDate())
            binding.systemNotificationMessage.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(notification.getMessage()))
            binding.systemNotificationMessage.movementMethod = LinkMovementMethod.getInstance()
            binding.systemNotificationMessage.transformationMethod = CustomTabTransformationMethod(binding.systemNotificationMessage.autoLinkMask)
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
