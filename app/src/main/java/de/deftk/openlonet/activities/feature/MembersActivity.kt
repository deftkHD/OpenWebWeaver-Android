package de.deftk.openlonet.activities.feature

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import de.deftk.lonet.api.implementation.Group
import de.deftk.lonet.api.model.IScope
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.MemberAdapter
import de.deftk.openlonet.databinding.ActivityMembersBinding
import de.deftk.openlonet.utils.getJsonExtra
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

    private lateinit var binding: ActivityMembersBinding
    private lateinit var group: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMembersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra(EXTRA_GROUP)) {
            group = intent.getJsonExtra(EXTRA_GROUP)!!

            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.title = group.name

            binding.membersSwipeRefresh.setOnRefreshListener {
                reloadMembers()
            }
            binding.membersList.setOnItemClickListener { _, view, _, _ ->
                openContextMenu(view)
            }
            registerForContextMenu(binding.membersList)
            reloadMembers()
        } else {
            finish()
        }
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
                (binding.membersList.adapter as Filterable).filter.filter(newText)
                return false
            }
        })

        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is AdapterView.AdapterContextMenuInfo) {
            val member = binding.membersList.adapter?.getItem(menuInfo.position) as IScope
            if (member.login != AuthStore.getApiUser().login) {
                menuInflater.inflate(R.menu.member_action_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.member_action_write_mail -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val member = binding.membersList.adapter?.getItem(info.position) as IScope
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(member.login)}"))
                startActivity(intent)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun reloadMembers() {
        binding.membersList.adapter = null
        binding.membersEmpty.visibility = TextView.GONE
        CoroutineScope(Dispatchers.IO).launch {
            loadMembers()
        }
    }

    private suspend fun loadMembers() {
        try {
            val members = group.getMembers(context = group.getRequestContext(AuthStore.getApiContext()))
            withContext(Dispatchers.Main) {
                binding.membersList.adapter = MemberAdapter(this@MembersActivity, members)
                binding.membersEmpty.isVisible = members.isEmpty()
                binding.membersSwipeRefresh.isRefreshing = false
                binding.progressMembers.visibility = ProgressBar.INVISIBLE
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.membersEmpty.visibility = TextView.GONE
                binding.membersSwipeRefresh.isRefreshing = false
                binding.progressMembers.visibility = ProgressBar.INVISIBLE
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