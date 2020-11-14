package de.deftk.lonet.mobile.activities.feature.board

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.board.BoardNotification
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.utils.TextUtils
import kotlinx.android.synthetic.main.activity_read_notification.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat

class ReadNotificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTIFICATION = "de.deftk.lonet.mobile.notification.notification_extra"

        const val ACTIVITY_RESULT_DELETE = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_notification)

        // back button in toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.see_notification)

        val notification = intent.getSerializableExtra(EXTRA_NOTIFICATION) as? BoardNotification?

        if (notification != null) {
            displayNotification(notification)

            if (notification.operator.effectiveRights.contains(Permission.BOARD_ADMIN)) {
                fab_edit_notification.visibility = View.VISIBLE
                fab_edit_notification.setOnClickListener {
                    val intent = Intent(this, EditNotificationActivity::class.java)
                    intent.putExtra(EditNotificationActivity.EXTRA_NOTIFICATION, notification)
                    startActivityForResult(intent, EditNotificationActivity.ACTIVITY_RESULT_EDIT)
                }
            } else {
                fab_edit_notification.visibility = View.INVISIBLE
            }
        } else {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return if ((intent.getSerializableExtra(EXTRA_NOTIFICATION) as BoardNotification).operator.effectiveRights.contains(Permission.BOARD_ADMIN)) {
            menuInflater.inflate(R.menu.board_item_menu, menu)
            true
        } else {
            super.onCreateOptionsMenu(menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.board_item_menu_edit -> {
                val intent = Intent(this, EditNotificationActivity::class.java)
                intent.putExtra(EditNotificationActivity.EXTRA_NOTIFICATION, this.intent.getSerializableExtra(EXTRA_NOTIFICATION) as BoardNotification)
                startActivityForResult(intent, EditNotificationActivity.ACTIVITY_RESULT_EDIT)
                true
            }
            R.id.board_item_menu_delete -> {
                val notification = intent.getSerializableExtra(EXTRA_NOTIFICATION) as BoardNotification
                CoroutineScope(Dispatchers.IO).launch {
                    notification.delete()

                    withContext(Dispatchers.Main) {
                        val intent = Intent()
                        intent.putExtra(EditNotificationActivity.EXTRA_NOTIFICATION, notification)
                        setResult(ACTIVITY_RESULT_DELETE, intent)
                        finish()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == EditNotificationActivity.ACTIVITY_RESULT_EDIT && data != null) {
            val notification = data.getSerializableExtra(EditNotificationActivity.EXTRA_NOTIFICATION) as BoardNotification
            displayNotification(notification)
            setResult(EditNotificationActivity.ACTIVITY_RESULT_EDIT, data)
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    private fun displayNotification(notification: BoardNotification) {
        notification_title.text = notification.title ?: ""
        notification_author.text = notification.creationMember.getName()
        notification_group.text = notification.operator.getName()
        notification_date.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(notification.creationDate)
        notification_text.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(notification.text))
        notification_text.movementMethod = LinkMovementMethod.getInstance()
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
