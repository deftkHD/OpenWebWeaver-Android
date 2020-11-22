package de.deftk.openlonet.activities.feature

import android.os.Bundle
import android.view.Menu
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import de.deftk.lonet.api.model.Group
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.MemberAdapter
import kotlinx.android.synthetic.main.activity_members.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity holding a list of all members for a specific group
 */
class MembersActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GROUP = "de.deftk.openlonet.members.group_extra"
    }

    private lateinit var group: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_members)

        val extraGroup = intent.getSerializableExtra(EXTRA_GROUP) as? Group?
        if (extraGroup != null) {
            group = extraGroup
        } else {
            finish()
            return
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = extraGroup.fullName ?: extraGroup.getName()

        members_swipe_refresh.setOnRefreshListener {
            reloadMembers()
        }
        members_list.setOnItemClickListener { _, _, position, _ ->
            val item = members_list.getItemAtPosition(position)
            //TODO show context menu
        }
        reloadMembers()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                (members_list.adapter as Filterable).filter.filter(newText)
                return false
            }
        })

        return true
    }

    private fun reloadMembers() {
        members_list.adapter = null
        members_empty.visibility = TextView.GONE
        CoroutineScope(Dispatchers.IO).launch {
            loadMembers()
        }
    }

    private suspend fun loadMembers() {
        try {
            val members = group.getMembers()
            withContext(Dispatchers.Main) {
                members_list?.adapter = MemberAdapter(this@MembersActivity, members)
                members_empty?.isVisible = members.isEmpty()
                members_swipe_refresh?.isRefreshing = false
                progress_members?.visibility = ProgressBar.INVISIBLE
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                members_empty.visibility = TextView.GONE
                members_swipe_refresh?.isRefreshing = false
                progress_members?.visibility = ProgressBar.INVISIBLE
                Toast.makeText(this@MembersActivity, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
            }
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}