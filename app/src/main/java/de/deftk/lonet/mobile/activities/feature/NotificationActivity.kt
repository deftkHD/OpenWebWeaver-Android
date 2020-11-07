package de.deftk.lonet.mobile.activities.feature

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.feature.board.BoardNotification
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.utils.TextUtils
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
        supportActionBar?.setTitle(R.string.see_notification)

        val notification = intent.getSerializableExtra(EXTRA_NOTIFICATION) as? BoardNotification

        if (notification != null) {
            notification_title.text = notification.title ?: ""
            notification_author.text = notification.creationMember.getName()
            notification_group.text = notification.operator.getName()
            notification_date.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(notification.creationDate)
            notification_text.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(notification.text))
            notification_text.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
