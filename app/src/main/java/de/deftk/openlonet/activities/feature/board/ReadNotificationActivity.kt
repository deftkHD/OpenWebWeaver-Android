package de.deftk.openlonet.activities.feature.board

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.implementation.Group
import de.deftk.lonet.api.implementation.feature.board.BoardNotification
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.board.BoardType
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.databinding.ActivityReadNotificationBinding
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.getJsonExtra
import de.deftk.openlonet.utils.putJsonExtra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat

class ReadNotificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTIFICATION = "de.deftk.openlonet.notification.notification_extra"
        const val EXTRA_GROUP = "de.deftk.openlonet.notification.group_extra"

        const val ACTIVITY_RESULT_DELETE = 4
    }

    private lateinit var binding: ActivityReadNotificationBinding

    private lateinit var notification: BoardNotification
    private lateinit var group: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // back button in toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.see_notification)

        if (intent.hasExtra(EXTRA_NOTIFICATION) && intent.hasExtra(EXTRA_GROUP)) {
            notification = intent.getJsonExtra(EXTRA_NOTIFICATION)!!
            group = intent.getJsonExtra(EXTRA_GROUP)!!

            displayNotification(notification, group)

            if (group.effectiveRights.contains(Permission.BOARD_ADMIN)) {
                binding.fabEditNotification.visibility = View.VISIBLE
                binding.fabEditNotification.setOnClickListener {
                    val intent = Intent(this, EditNotificationActivity::class.java)
                    intent.putJsonExtra(EditNotificationActivity.EXTRA_NOTIFICATION, notification)
                    intent.putJsonExtra(EditNotificationActivity.EXTRA_GROUP, group)
                    startActivityForResult(intent, EditNotificationActivity.ACTIVITY_RESULT_EDIT)
                }
            } else {
                binding.fabEditNotification.visibility = View.INVISIBLE
            }
        } else {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return if (group.effectiveRights.contains(Permission.BOARD_ADMIN)) {
            menuInflater.inflate(R.menu.simple_edit_item_menu, menu)
            true
        } else {
            super.onCreateOptionsMenu(menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                val intent = Intent(this, EditNotificationActivity::class.java)
                intent.putJsonExtra(EditNotificationActivity.EXTRA_NOTIFICATION, notification)
                intent.putJsonExtra(EditNotificationActivity.EXTRA_GROUP, group)
                startActivityForResult(intent, EditNotificationActivity.ACTIVITY_RESULT_EDIT)
                true
            }
            R.id.menu_item_delete -> {
                CoroutineScope(Dispatchers.IO).launch {
                    notification.delete(BoardType.ALL, group.getRequestContext(AuthStore.getApiContext()))

                    withContext(Dispatchers.Main) {
                        val intent = Intent()
                        intent.putJsonExtra(EXTRA_NOTIFICATION, notification)
                        intent.putJsonExtra(EXTRA_GROUP, group)
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
            val notification = data.getJsonExtra<BoardNotification>(EditNotificationActivity.EXTRA_NOTIFICATION)!!
            val group = data.getJsonExtra<Group>(EditNotificationActivity.EXTRA_GROUP)!!
            displayNotification(notification, group)
            setResult(EditNotificationActivity.ACTIVITY_RESULT_EDIT, data)
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    private fun displayNotification(notification: BoardNotification, group: Group) {
        binding.notificationTitle.text = notification.getTitle()
        binding.notificationAuthor.text = notification.getCreated().member.name
        binding.notificationGroup.text = group.name
        binding.notificationDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(notification.getCreated().date)
        binding.notificationText.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(notification.getText()))
        binding.notificationText.movementMethod = LinkMovementMethod.getInstance()
        binding.notificationText.transformationMethod = CustomTabTransformationMethod(binding.notificationText.autoLinkMask)
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
