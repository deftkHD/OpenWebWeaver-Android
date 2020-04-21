package de.deftk.lonet.mobile.activities.feature

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.feature.Notification
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import kotlinx.android.synthetic.main.activity_notification.*
import java.text.DateFormat

class NotificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTIFICATION = "de.deftk.lonet.mobile.notification.notification_extra"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        // back button in toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val notification = intent.getSerializableExtra(EXTRA_NOTIFICATION) as? Notification

        if (notification != null) {
            notification_title.text = notification.title ?: ""
            notification_author.text = notification.creationMember.name ?: notification.creationMember.login
            notification_date.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(notification.creationDate)
            notification_text.text = notification.text ?: "" //FIXME text sometimes contains internal references
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
