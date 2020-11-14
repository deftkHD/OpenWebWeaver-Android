package de.deftk.lonet.mobile.activities.feature.board

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.Group
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.board.BoardNotification
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.adapter.NotificationAdapter
import de.deftk.lonet.mobile.fragments.NotificationsFragment
import de.deftk.lonet.mobile.utils.TextUtils
import kotlinx.android.synthetic.main.activity_edit_notification.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditNotificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTIFICATION = "de.deftk.lonet.mobile.notification.notification_extra"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_notification)

        // back button in toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val notification = intent.getSerializableExtra(EXTRA_NOTIFICATION) as? BoardNotification

        val effectiveGroups = AuthStore.appUser.groups.filter { it.effectiveRights.contains(Permission.BOARD_ADMIN) }
        notification_group?.adapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, effectiveGroups.map { it.getLogin() })

        val colors = NotificationAdapter.BoardNotificationColors.values()
        notification_accent?.adapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, colors.map { getString(it.text) })

        if (notification != null) {
            // edit existing notification
            supportActionBar?.setTitle(R.string.edit_notification)
            notification_title?.setText(notification.title ?: "")
            notification_text?.setText(TextUtils.parseInternalReferences(TextUtils.parseHtml(notification.text)))
            notification_text?.movementMethod = LinkMovementMethod.getInstance()
            notification_group?.setSelection(effectiveGroups.indexOf(notification.operator as Group))
            notification_group.isEnabled = false
            val index = colors.indexOf(NotificationAdapter.BoardNotificationColors.getByApiColor(notification.color))
            notification_accent?.setSelection(index)
        } else {
            // create new notification
            supportActionBar?.setTitle(R.string.add_new_notification)
            notification_group.isEnabled = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_save_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.save) {
            val title = notification_title.text.toString()
            val group = notification_group.selectedItem
            val color = notification_accent.selectedItemPosition
            val text = notification_text.text.toString()
            CoroutineScope(Dispatchers.IO).launch {
                val notification = intent.getSerializableExtra(EXTRA_NOTIFICATION) as? BoardNotification?
                if (notification != null) {
                    notification.edit(
                        title,
                        text,
                        NotificationAdapter.BoardNotificationColors.values()[color].apiColor
                    )
                    withContext(Dispatchers.Main) {
                        val intent = Intent()
                        intent.putExtra(EXTRA_NOTIFICATION, notification)
                        setResult(NotificationsFragment.ACTIVITY_REQUEST_EDIT, intent)
                        finish()
                    }
                } else {
                    val newNotification = AuthStore.appUser.groups.firstOrNull { it.getLogin() == group }
                        ?.addBoardNotification(title, text, NotificationAdapter.BoardNotificationColors.values()[color].apiColor)
                    withContext(Dispatchers.Main) {
                        val intent = Intent()
                        intent.putExtra(EXTRA_NOTIFICATION, newNotification)
                        setResult(NotificationsFragment.ACTIVITY_REQUEST_ADD, intent)
                        finish()
                    }
                }
            }
            return true
        }
        return false
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}