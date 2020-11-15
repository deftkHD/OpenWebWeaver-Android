package de.deftk.lonet.mobile.activities.feature.tasks

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.Task
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.utils.TextUtils
import kotlinx.android.synthetic.main.activity_read_task.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat

class ReadTaskActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK = "de.deftk.lonet.mobile.task.task_extra"

        const val ACTIVITY_RESULT_DELETE = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_task)

        // back button in toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.task_details)

        val task = intent.getSerializableExtra(EXTRA_TASK) as? Task

        if (task != null) {
            displayTask(task)
        } else {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return if ((intent.getSerializableExtra(EXTRA_TASK) as Task).operator.effectiveRights.contains(Permission.TASKS_ADMIN)) {
            menuInflater.inflate(R.menu.simple_edit_item_menu, menu)
            true
        } else super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                val intent = Intent(this, EditTaskActivity::class.java)
                intent.putExtra(EditTaskActivity.EXTRA_TASK, this.intent.getSerializableExtra(EXTRA_TASK) as Task)
                startActivityForResult(intent, EditTaskActivity.ACTIVITY_RESULT_EDIT)
                true
            }
            R.id.menu_item_delete -> {
                val task = intent.getSerializableExtra(EXTRA_TASK) as Task
                CoroutineScope(Dispatchers.IO).launch {
                    task.delete()

                    withContext(Dispatchers.Main) {
                        val intent = Intent()
                        intent.putExtra(EditTaskActivity.EXTRA_TASK, task)
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
            val task = data.getSerializableExtra(EditTaskActivity.EXTRA_TASK) as Task
            displayTask(task)
            setResult(EditTaskActivity.ACTIVITY_RESULT_EDIT, data)
        } else return super.onActivityResult(requestCode, resultCode, data)
    }

    private fun displayTask(task: Task) {
        task_title.text = task.title
        task_author.text = task.creationMember.getName()
        task_group.text = task.operator.getName()
        task_created.text = String.format(getString(R.string.created_date), DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(task.creationDate))
        task_due.text = String.format(getString(R.string.until_date), if (task.endDate != null) DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(task.endDate!!) else getString(R.string.not_set))
        task_detail.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(task.description))
        task_detail.movementMethod = LinkMovementMethod.getInstance()
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
