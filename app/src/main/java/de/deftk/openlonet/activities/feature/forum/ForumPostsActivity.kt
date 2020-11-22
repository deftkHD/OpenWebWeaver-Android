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
import kotlinx.android.synthetic.main.activity_forum_posts.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

/**
 * Activity holding a list of all forum posts for a specific group
 */
class ForumPostsActivity : AppCompatActivity() {

    //TODO icons for pinned & locked

    companion object {
        const val EXTRA_GROUP = "de.deftk.openlonet.forum.group_extra"
    }

    private lateinit var group: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forum_posts)

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

        forum_swipe_refresh.setOnRefreshListener {
            reloadForumPosts()
        }
        forum_list.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, ForumPostActivity::class.java)
            intent.putExtra(ForumPostActivity.EXTRA_FORUM_POST, forum_list.getItemAtPosition(position) as Serializable)
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
                (forum_list.adapter as Filterable).filter.filter(newText)
                return false
            }
        })

        return true
    }

    private fun reloadForumPosts() {
        forum_list.adapter = null
        forum_empty.visibility = TextView.GONE
        CoroutineScope(Dispatchers.IO).launch {
            loadForumPosts()
        }
    }

    private suspend fun loadForumPosts() {
        try {
            val posts = group.getForumPosts()
            withContext(Dispatchers.Main) {
                forum_list.adapter = ForumPostAdapter(this@ForumPostsActivity, posts)
                forum_empty.isVisible = posts.isEmpty()
                progress_forum.visibility = ProgressBar.GONE
                forum_swipe_refresh.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                forum_empty.visibility = TextView.GONE
                progress_forum.visibility = ProgressBar.GONE
                forum_swipe_refresh.isRefreshing = false
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