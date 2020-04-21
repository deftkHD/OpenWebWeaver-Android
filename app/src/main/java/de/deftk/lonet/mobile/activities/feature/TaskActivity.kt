package de.deftk.lonet.mobile.activities.feature

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.feature.Task
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.utils.TextUtils
import kotlinx.android.synthetic.main.activity_task.*
import java.text.DateFormat

class TaskActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK = "de.deftk.lonet.mobile.task.task_extra"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        // back button in toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.task_details)

        val task = intent.getSerializableExtra(EXTRA_TASK) as? Task

        if (task != null) {
            task_title.text = task.title
            task_author.text = task.creationMember.name ?: task.creationMember.fullName ?: task.creationMember.login
            task_group.text = task.group.name ?: task.group.login
            task_created.text = String.format(getString(R.string.created_date), DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(task.creationDate))
            task_due.text = String.format(getString(R.string.until_date), if (task.endDate != null) DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(task.endDate!!) else getString(R.string.not_set))
            task_detail.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(task.description))
            task_detail.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
