package de.deftk.lonet.mobile.activities.feature.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.Group
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.Task
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.utils.TextUtils
import kotlinx.android.synthetic.main.activity_edit_task.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class EditTaskActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK = "de.deftk.lonet.mobile.task.task_extra"

        const val ACTIVITY_RESULT_ADD = 2
        const val ACTIVITY_RESULT_EDIT = 3
    }

    private var startDate: Date? = null
    private var dueDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_task)

        // back button in toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val task = intent.getSerializableExtra(EXTRA_TASK) as? Task?

        val effectiveGroups = AuthStore.appUser.groups.filter { it.effectiveRights.contains(Permission.TASKS_ADMIN) }
        task_group.adapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, effectiveGroups.map { it.getLogin() })

        if (task != null) {
            // edit existing task
            supportActionBar?.setTitle(R.string.edit_task)
            task_title.setText(task.title ?: "")
            task_completed.isChecked = task.completed
            startDate = task.startDate
            if (startDate != null) {
                task_start.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(startDate!!))
            }
            dueDate = task.endDate
            if (dueDate != null) {
                task_due.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(dueDate!!))
            }
            task_group.setSelection(effectiveGroups.indexOf(task.operator as Group))
            task_group.isEnabled = true
            task_text.setText(TextUtils.parseInternalReferences(TextUtils.parseHtml(task.description)))
            task_text.movementMethod = LinkMovementMethod.getInstance()
        } else {
            // create new task
            supportActionBar?.setTitle(R.string.add_new_task)
            task_group.isEnabled = true
        }

        task_start.inputType = InputType.TYPE_NULL
        task_start.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (task_start.text.toString().isNotBlank())
                calendar.time = SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).parse(task_start.text.toString()) ?: Date()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                TimePickerDialog(this, { _, hour, minute ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)

                    if (dueDate != null && calendar.timeInMillis > dueDate!!.time) {
                        Toast.makeText(this, R.string.task_start_before_due, Toast.LENGTH_SHORT).show()
                    } else {
                        startDate = calendar.time
                        task_start.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(startDate!!))
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        task_due.inputType = InputType.TYPE_NULL
        task_due.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (task_due.text.toString().isNotBlank())
                calendar.time = SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).parse(task_due.text.toString()) ?: Date()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                TimePickerDialog(this, { _, hour, minute ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)

                    if (startDate != null && calendar.timeInMillis < startDate!!.time) {
                        Toast.makeText(this, R.string.task_start_before_due, Toast.LENGTH_SHORT).show()
                    } else {
                        dueDate = calendar.time
                        task_due.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(dueDate!!))
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_save_menu, menu)
        return true
    }

    //TODO ability to remove start/due date
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.save) {
            val title = task_title.text.toString()
            val group = task_group.selectedItem
            val completed = task_completed.isChecked
            val description = task_text.text.toString()
            CoroutineScope(Dispatchers.IO).launch {
                val task = intent.getSerializableExtra(EXTRA_TASK) as? Task?
                if (task != null) {
                    task.edit(completed, description, dueDate, startDate, title)
                    withContext(Dispatchers.Main) {
                        val intent = Intent()
                        intent.putExtra(EXTRA_TASK, task)
                        setResult(ACTIVITY_RESULT_EDIT, intent)
                        finish()
                    }
                } else {
                    val newTask = AuthStore.appUser.groups.firstOrNull { it.getLogin() == group }?.addTask(title, completed, description, dueDate, startDate)
                    withContext(Dispatchers.Main) {
                        val intent = Intent()
                        intent.putExtra(EXTRA_TASK, newTask)
                        setResult(ACTIVITY_RESULT_ADD, intent)
                        finish()
                    }
                }
            }
            return true
        }
        return false
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }



}