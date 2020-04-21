package de.deftk.lonet.mobile.activities.feature

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.feature.Task
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
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

        val task = intent.getSerializableExtra(EXTRA_TASK) as? Task

        if (task != null) {
            findViewById<TextView>(R.id.task_title).text = task.title
            findViewById<TextView>(R.id.task_author).text = task.creationMember.name ?: task.creationMember.fullName ?: task.creationMember.login
            findViewById<TextView>(R.id.task_created).text = String.format(getString(R.string.created_date), DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(task.creationDate))
            findViewById<TextView>(R.id.task_due).text = String.format(getString(R.string.until_date), if (task.endDate != null) DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(task.endDate!!) else getString(R.string.not_set))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                findViewById<TextView>(R.id.task_detail).text = Html.fromHtml(task.description, Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION") // is legit
                findViewById<TextView>(R.id.task_detail).text = Html.fromHtml(task.description)
            }
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
