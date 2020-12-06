package de.deftk.openlonet.activities.feature

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.feature.SystemNotification
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.SystemNotificationAdapter
import de.deftk.openlonet.databinding.ActivitySystemNotificationBinding
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
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

        val notification = intent.getSerializableExtra(EXTRA_SYSTEM_NOTIFICATION) as? SystemNotification

        if (notification != null) {
            val type = notification.messageType
            binding.systemNotificationTitle.text = if (type != null) {
                getString(SystemNotificationAdapter.typeTranslationMap[type] ?: R.string.system_notification_type_unknown)
            } else {
                getString(R.string.system_notification_type_unknown)
            }
            binding.systemNotificationAuthor.text = notification.member.getName()
            binding.systemNotificationGroup.text = notification.group.getName()
            binding.systemNotificationDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(notification.date)
            binding.systemNotificationMessage.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(notification.message))
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
