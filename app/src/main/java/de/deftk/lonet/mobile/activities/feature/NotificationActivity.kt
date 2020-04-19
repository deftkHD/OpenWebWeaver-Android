package de.deftk.lonet.mobile.activities.feature

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import kotlinx.android.synthetic.main.activity_notification.*
import java.text.DateFormat

class NotificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_HASHCODE = "extra_hashcode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        // back button in toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val hashcode = intent.getIntExtra(SystemNotificationActivity.EXTRA_HASHCODE, -1)
        val notification = AuthStore.appUser.getNotifications().firstOrNull { it.hashCode() == hashcode } ?: error("Notification disappeared (Hashcode $hashcode)")

        notification_title.text = notification.title ?: ""
        notification_author.text = notification.creationMember.name ?: notification.creationMember.login
        notification_date.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(notification.creationDate)
        notification_text.text = notification.text ?: "" //FIXME text sometimes contains internal references
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
