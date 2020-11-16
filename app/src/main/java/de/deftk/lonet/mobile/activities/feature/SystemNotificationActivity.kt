package de.deftk.lonet.mobile.activities.feature

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.feature.SystemNotification
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.adapter.SystemNotificationAdapter
import de.deftk.lonet.mobile.utils.TextUtils
import kotlinx.android.synthetic.main.activity_system_notification.*
import java.text.DateFormat

class SystemNotificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SYSTEM_NOTIFICATION = "de.deftk.lonet.mobile.system_notifications.system_notification_extra"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_notification)

        // back button in toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.see_system_notification)

        val notification = intent.getSerializableExtra(EXTRA_SYSTEM_NOTIFICATION) as? SystemNotification

        if (notification != null) {
            val type = notification.messageType
            system_notification_title.text = if (type != null) {
                getString(SystemNotificationAdapter.typeTranslationMap[type] ?: R.string.system_notification_type_unknown)
            } else {
                getString(R.string.system_notification_type_unknown)
            }
            system_notification_author.text = notification.member.getName()
            system_notification_group.text = notification.group.getName()
            system_notification_date.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(notification.date)
            system_notification_message.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(notification.message))
            system_notification_message.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
