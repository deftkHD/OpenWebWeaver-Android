package de.deftk.openlonet.activities.feature.forum

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import de.deftk.lonet.api.model.Group
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.ForumPostAdapter
import de.deftk.openlonet.databinding.ActivityForumPostsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

/**
 * Activity holding a list of all forum posts for a specific group
 */
class ForumPostsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GROUP = "de.deftk.openlonet.forum.group_extra"
    }

    private lateinit var binding: ActivityForumPostsBinding
    private lateinit var group: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForumPostsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extraGroup = intent.getSerializableExtra(EXTRA_GROUP) as? Group?
        if (extraGroup != null) {
            group = extraGroup
        } else {
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = extraGroup.fullName ?: extraGroup.getName()

        binding.forumSwipeRefresh.setOnRefreshListener {
            reloadForumPosts()
        }
        binding.forumList.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, ForumPostActivity::class.java)
            intent.putExtra(ForumPostActivity.EXTRA_FORUM_POST, binding.forumList.getItemAtPosition(position) as Serializable)
            startActivity(intent)
        }

        reloadForumPosts()
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
                (binding.forumList.adapter as Filterable).filter.filter(newText)
                return false
            }
        })

        return true
    }

    private fun reloadForumPosts() {
        binding.forumList.adapter = null
        binding.forumEmpty.visibility = TextView.GONE
        CoroutineScope(Dispatchers.IO).launch {
            loadForumPosts()
        }
    }

    private suspend fun loadForumPosts() {
        try {
            val posts = group.getForumPosts()
            withContext(Dispatchers.Main) {
                binding.forumList.adapter = ForumPostAdapter(this@ForumPostsActivity, posts)
                binding.forumEmpty.isVisible = posts.isEmpty()
                binding.progressForum.visibility = ProgressBar.GONE
                binding.forumSwipeRefresh.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.forumEmpty.visibility = TextView.GONE
                binding.progressForum.visibility = ProgressBar.GONE
                binding.forumSwipeRefresh.isRefreshing = false
                Toast.makeText(this@ForumPostsActivity, getString(R.string.request_failed_other).format("No details"), Toast.LENGTH_LONG).show()
            }
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}