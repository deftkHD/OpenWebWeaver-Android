package de.deftk.openlonet.activities.feature.board

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.board.BoardNotification
import de.deftk.openlonet.R
import de.deftk.openlonet.databinding.ActivityReadNotificationBinding
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat

class ReadNotificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTIFICATION = "de.deftk.openlonet.notification.notification_extra"

        const val ACTIVITY_RESULT_DELETE = 4
    }

    private lateinit var binding: ActivityReadNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // back button in toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.see_notification)

        val notification = intent.getSerializableExtra(EXTRA_NOTIFICATION) as? BoardNotification?

        if (notification != null) {
            displayNotification(notification)

            if (notification.operator.effectiveRights.contains(Permission.BOARD_ADMIN)) {
                binding.fabEditNotification.visibility = View.VISIBLE
                binding.fabEditNotification.setOnClickListener {
                    val intent = Intent(this, EditNotificationActivity::class.java)
                    intent.putExtra(EditNotificationActivity.EXTRA_NOTIFICATION, notification)
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
        return if ((intent.getSerializableExtra(EXTRA_NOTIFICATION) as BoardNotification).operator.effectiveRights.contains(Permission.BOARD_ADMIN)) {
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
                intent.putExtra(EditNotificationActivity.EXTRA_NOTIFICATION, this.intent.getSerializableExtra(EXTRA_NOTIFICATION) as BoardNotification)
                startActivityForResult(intent, EditNotificationActivity.ACTIVITY_RESULT_EDIT)
                true
            }
            R.id.menu_item_delete -> {
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
        binding.notificationTitle.text = notification.title ?: ""
        binding.notificationAuthor.text = notification.creationMember.getName()
        binding.notificationGroup.text = notification.operator.getName()
        binding.notificationDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(notification.creationDate)
        binding.notificationText.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(notification.text))
        binding.notificationText.movementMethod = LinkMovementMethod.getInstance()
        binding.notificationText.transformationMethod = CustomTabTransformationMethod(binding.notificationText.autoLinkMask)
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
