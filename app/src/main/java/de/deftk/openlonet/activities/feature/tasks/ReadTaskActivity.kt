package de.deftk.openlonet.activities.feature.tasks

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.implementation.Group
import de.deftk.lonet.api.implementation.feature.tasks.Task
import de.deftk.lonet.api.model.Permission
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.databinding.ActivityReadTaskBinding
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.getJsonExtra
import de.deftk.openlonet.utils.putJsonExtra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat

class ReadTaskActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK = "de.deftk.openlonet.task.task_extra"
        const val EXTRA_GROUP = "de.deftk.openlonet.task.group_extra"

        const val ACTIVITY_RESULT_DELETE = 4
    }

    private lateinit var binding: ActivityReadTaskBinding

    private lateinit var task: Task
    private lateinit var group: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // back button in toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.task_details)

        if (intent.hasExtra(EXTRA_TASK) && intent.hasExtra(EXTRA_GROUP)) {
            task = intent.getJsonExtra(EXTRA_TASK)!!
            group = intent.getJsonExtra(EXTRA_GROUP)!!
            displayTask(task)
        } else {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (group.effectiveRights.contains(Permission.TASKS_ADMIN))
            menuInflater.inflate(R.menu.simple_edit_item_menu, menu)
        menuInflater.inflate(R.menu.read_task_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_import_in_calendar -> {
                val intent = Intent(Intent.ACTION_INSERT)
                intent.data = CalendarContract.Events.CONTENT_URI
                intent.putExtra(CalendarContract.Events.TITLE, task.getTitle())
                if (task.getStartDate() != null)
                    intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, task.getStartDate()!!.time)
                if (task.getEndDate() != null)
                    intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, task.getEndDate()!!.time)
                if (task.getDescription() != null)
                    intent.putExtra(CalendarContract.Events.DESCRIPTION, task.getDescription())
                startActivity(intent)
                true
            }
            R.id.menu_item_edit -> {
                val intent = Intent(this, EditTaskActivity::class.java)
                intent.putJsonExtra(EditTaskActivity.EXTRA_TASK, task)
                intent.putJsonExtra(EditTaskActivity.EXTRA_GROUP, group)
                startActivityForResult(intent, EditTaskActivity.ACTIVITY_RESULT_EDIT)
                true
            }
            R.id.menu_item_delete -> {
                CoroutineScope(Dispatchers.IO).launch {
                    task.delete(group.getRequestContext(AuthStore.getApiContext()))

                    withContext(Dispatchers.Main) {
                        val intent = Intent()
                        intent.putJsonExtra(EditTaskActivity.EXTRA_TASK, task)
                        intent.putJsonExtra(EditTaskActivity.EXTRA_GROUP, group)
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
        if (resultCode == EditTaskActivity.ACTIVITY_RESULT_EDIT && data != null) {
            task = data.getJsonExtra(EXTRA_TASK)!!
            group = data.getJsonExtra(EXTRA_GROUP)!!
            displayTask(task)
            setResult(EditTaskActivity.ACTIVITY_RESULT_EDIT, data)
        } else return super.onActivityResult(requestCode, resultCode, data)
    }

    private fun displayTask(task: Task) {
        binding.taskTitle.text = task.getTitle()
        binding.taskAuthor.text = task.created.member.name
        binding.taskGroup.text = group.name
        binding.taskCreated.text = String.format(getString(R.string.created_date), DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(task.created.date))
        binding.taskDue.text = String.format(getString(R.string.until_date), if (task.getEndDate() != null) DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(task.getEndDate()!!) else getString(R.string.not_set))
        binding.taskDetail.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(task.getDescription()))
        binding.taskDetail.movementMethod = LinkMovementMethod.getInstance()
        binding.taskDetail.transformationMethod = CustomTabTransformationMethod(binding.taskDetail.autoLinkMask)
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
