package de.deftk.lonet.mobile.activities.feature

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.adapter.SystemNotificationAdapter
import kotlinx.android.synthetic.main.activity_system_notification.*
import java.text.DateFormat

class SystemNotificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_HASHCODE = "extra_hashcode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_notification)

        // back button in toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val hashcode = intent.getIntExtra(EXTRA_HASHCODE, -1)
        val notification = AuthStore.appUser.getSystemNofications().firstOrNull { it.hashCode() == hashcode } ?: error("Systemnotification disappeared (Hashcode $hashcode)")

        system_notification_title.text = getString(SystemNotificationAdapter.typeTranslationMap.getValue(notification.messageType))
        system_notification_author.text = notification.member.name ?: notification.member.login
        system_notification_group.text = notification.group.name ?: notification.group.login
        system_notification_date.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(notification.date)
        system_notification_message.text = notification.message
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
