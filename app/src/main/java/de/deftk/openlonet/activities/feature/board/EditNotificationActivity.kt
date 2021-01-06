package de.deftk.openlonet.activities.feature.board

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.implementation.Group
import de.deftk.lonet.api.implementation.feature.board.BoardNotification
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.board.BoardType
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.NotificationAdapter
import de.deftk.openlonet.databinding.ActivityEditNotificationBinding
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.getJsonExtra
import de.deftk.openlonet.utils.putJsonExtra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditNotificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTIFICATION = "de.deftk.openlonet.notification.notification_extra"
        const val EXTRA_GROUP = "de.deftk.openlonet.notification.group_extra"

        const val ACTIVITY_RESULT_ADD = 2
        const val ACTIVITY_RESULT_EDIT = 3
    }

    private lateinit var binding: ActivityEditNotificationBinding

    private var notification: BoardNotification? = null
    private var group: Group? = null

    //TODO allow selecting board

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // back button in toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val effectiveGroups = AuthStore.getApiUser().getGroups().filter { it.effectiveRights.contains(Permission.BOARD_ADMIN) }
        binding.notificationGroup.adapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, effectiveGroups.map { it.login })

        val colors = NotificationAdapter.BoardNotificationColors.values()
        binding.notificationAccent.adapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, colors.map { getString(it.text) })

        if (intent.hasExtra(EXTRA_NOTIFICATION) && intent.hasExtra(EXTRA_GROUP)) {
            // edit existing notification
            notification = intent.getJsonExtra(EXTRA_NOTIFICATION)
            group = intent.getJsonExtra(EXTRA_GROUP)
            supportActionBar?.setTitle(R.string.edit_notification)
            binding.notificationTitle.setText(notification!!.getTitle())
            binding.notificationText.setText(TextUtils.parseInternalReferences(TextUtils.parseHtml(notification!!.getText())))
            binding.notificationText.movementMethod = LinkMovementMethod.getInstance()
            binding.notificationGroup.setSelection(effectiveGroups.indexOf(group))
            binding.notificationGroup.isEnabled = false
            val index = colors.indexOf(NotificationAdapter.BoardNotificationColors.getByApiColor(notification!!.getColor()))
            binding.notificationAccent.setSelection(index)
        } else {
            // create new notification
            supportActionBar?.setTitle(R.string.add_new_notification)
            binding.notificationGroup.isEnabled = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_save_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.save) {
            val title = binding.notificationTitle.text.toString()
            val selectedGroup = binding.notificationGroup.selectedItem
            val color = binding.notificationAccent.selectedItemPosition
            val text = binding.notificationText.text.toString()
            CoroutineScope(Dispatchers.IO).launch {
                if (notification != null && group != null) {
                    notification!!.edit(
                        title,
                        text,
                        NotificationAdapter.BoardNotificationColors.values()[color].apiColor,
                        killDate = null, //TODO allow setting kill date
                        BoardType.ALL,
                        group!!.getRequestContext(AuthStore.getApiContext())
                    )
                    withContext(Dispatchers.Main) {
                        val intent = Intent()
                        intent.putJsonExtra(EXTRA_NOTIFICATION, notification)
                        intent.putJsonExtra(EXTRA_GROUP, group)
                        setResult(ACTIVITY_RESULT_EDIT, intent)
                        finish()
                    }
                } else {
                    group = AuthStore.getApiUser().getGroups().firstOrNull { it.login == selectedGroup }
                    val newNotification = group?.addBoardNotification(
                        title,
                        text,
                        NotificationAdapter.BoardNotificationColors.values()[color].apiColor,
                        killDate = null,
                        group!!.getRequestContext(AuthStore.getApiContext())
                    )
                    withContext(Dispatchers.Main) {
                        val intent = Intent()
                        if (newNotification == null) {
                            setResult(RESULT_CANCELED, intent)
                        } else {
                            intent.putJsonExtra(EXTRA_NOTIFICATION, newNotification)
                            intent.putJsonExtra(EXTRA_GROUP, group)
                            setResult(ACTIVITY_RESULT_ADD, intent)
                        }
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