package de.deftk.openlonet.activities.feature.tasks

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
import de.deftk.lonet.api.implementation.Group
import de.deftk.lonet.api.implementation.feature.tasks.Task
import de.deftk.lonet.api.model.Permission
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.databinding.ActivityEditTaskBinding
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.getJsonExtra
import de.deftk.openlonet.utils.putJsonExtra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class EditTaskActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK = "de.deftk.openlonet.task.task_extra"
        const val EXTRA_GROUP = "de.deftk.openlonet.task.group_extra"

        const val ACTIVITY_RESULT_ADD = 2
        const val ACTIVITY_RESULT_EDIT = 3
    }

    private lateinit var binding: ActivityEditTaskBinding

    private var task: Task? = null
    private var group: Group? = null

    private var startDate: Date? = null
    private var dueDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // back button in toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val effectiveGroups = AuthStore.getApiUser().getGroups().filter { it.effectiveRights.contains(Permission.TASKS_ADMIN) }
        binding.taskGroup.adapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, effectiveGroups.map { it.login })

        if (intent.hasExtra(EXTRA_TASK) && intent.hasExtra(EXTRA_GROUP)) {
            task = intent.getJsonExtra(EXTRA_TASK)!!
            group = intent.getJsonExtra(EXTRA_GROUP)!!

            // edit existing task
            supportActionBar?.setTitle(R.string.edit_task)
            binding.taskTitle.setText(task!!.getTitle())
            binding.taskCompleted.isChecked = task!!.isCompleted()
            startDate = task!!.getStartDate()
            if (startDate != null) {
                binding.taskStart.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(startDate!!))
            }
            dueDate = task!!.getEndDate()
            if (dueDate != null) {
                binding.taskDue.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(dueDate!!))
            }
            binding.taskGroup.setSelection(effectiveGroups.indexOf(group))
            binding.taskGroup.isEnabled = true
            binding.taskText.setText(TextUtils.parseInternalReferences(TextUtils.parseHtml(task!!.getDescription())))
            binding.taskText.movementMethod = LinkMovementMethod.getInstance()
        } else {
            // create new task
            supportActionBar?.setTitle(R.string.add_new_task)
            binding.taskGroup.isEnabled = true
        }

        binding.taskStart.inputType = InputType.TYPE_NULL
        binding.taskStart.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (binding.taskStart.text.toString().isNotBlank())
                calendar.time = SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).parse(binding.taskStart.text.toString()) ?: Date()
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
                        binding.taskStart.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(startDate!!))
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.taskDue.inputType = InputType.TYPE_NULL
        binding.taskDue.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (binding.taskDue.text.toString().isNotBlank())
                calendar.time = SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).parse(binding.taskDue.text.toString()) ?: Date()
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
                        binding.taskDue.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(dueDate!!))
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
            val title = binding.taskTitle.text.toString()
            val selectedGroup = binding.taskGroup.selectedItem
            val completed = binding.taskCompleted.isChecked
            val description = binding.taskText.text.toString()
            CoroutineScope(Dispatchers.IO).launch {
                if (task != null && group != null) {
                    task!!.edit(
                        title = title,
                        description = description,
                        completed = completed,
                        startDate = startDate,
                        endDate = dueDate,
                        context = group!!.getRequestContext(AuthStore.getApiContext())
                    )

                    withContext(Dispatchers.Main) {
                        val intent = Intent()
                        intent.putJsonExtra(EXTRA_TASK, task)
                        intent.putJsonExtra(EXTRA_GROUP, group)
                        setResult(ACTIVITY_RESULT_EDIT, intent)
                        finish()
                    }
                } else {
                    group = AuthStore.getApiUser().getGroups().firstOrNull { it.login == selectedGroup }
                    val newTask = group?.addTask(
                        title = title,
                        description = description,
                        completed = completed,
                        startDate = startDate?.time,
                        dueDate = dueDate?.time,
                        context = group!!.getRequestContext(AuthStore.getApiContext())
                    )
                    withContext(Dispatchers.Main) {
                        val intent = Intent()
                        intent.putJsonExtra(EXTRA_TASK, newTask)
                        intent.putJsonExtra(EXTRA_GROUP, group)
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