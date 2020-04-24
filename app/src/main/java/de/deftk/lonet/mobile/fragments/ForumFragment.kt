package de.deftk.lonet.mobile.fragments

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.Group
import de.deftk.lonet.api.model.feature.forum.ForumPost
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.FeatureFragment
import de.deftk.lonet.mobile.abstract.IBackHandler
import de.deftk.lonet.mobile.activities.feature.ForumPostActivity
import de.deftk.lonet.mobile.adapter.ForumAdapter
import de.deftk.lonet.mobile.adapter.ForumPostAdapter
import de.deftk.lonet.mobile.feature.AppFeature
import kotlinx.android.synthetic.main.fragment_forum.*

class ForumFragment : FeatureFragment(AppFeature.FEATURE_FORUM), IBackHandler {

    //TODO icons for pinned & locked

    companion object {
        private const val SAVE_CURRENT_GROUP = "de.deftk.lonet.mobile.forum.current_group"
    }

    private var currentGroup: Group? = null
    private lateinit var list: ListView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progress: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (savedInstanceState != null) {
            currentGroup = savedInstanceState.getSerializable(SAVE_CURRENT_GROUP) as Group?
        }

        val view = inflater.inflate(R.layout.fragment_forum, container, false)
        list = view.findViewById(R.id.forum_list)
        progress = view.findViewById(R.id.progress_forum)
        swipeRefresh = view.findViewById(R.id.forum_swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            navigate(currentGroup, true)
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val item = list.getItemAtPosition(position)
            if (item is Group) {
                navigate(item, false)
            } else {
                item as ForumPost
                val intent = Intent(context, ForumPostActivity::class.java)
                intent.putExtra(ForumPostActivity.EXTRA_FORUM_POST, item)
                startActivity(intent)
            }
        }
        navigate(currentGroup, false)
        return view
    }

    private fun navigate(forum: Group?, overwriteCache: Boolean) {
        currentGroup = forum
        if (forum == null) {
            list.adapter = ForumAdapter(context ?: error("Oops, no context?"),
                AuthStore.appUser.getContext().getGroups().filter { Feature.FORUM.isAvailable(it.permissions) })
            swipeRefresh.isRefreshing = false
            progress.visibility = ProgressBar.INVISIBLE
        } else {
            list.adapter = null
            EntryLoader().execute(forum, overwriteCache)
        }
    }

    override fun onBackPressed(): Boolean {
        if (currentGroup != null) {
            navigate(null, false)
            return true
        }
        return false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(SAVE_CURRENT_GROUP, currentGroup)
    }

    private inner class EntryLoader : AsyncTask<Any, Void, List<ForumPost>?>() {

        override fun doInBackground(vararg params: Any): List<ForumPost>? {
            return try {
                (params[0] as Group).getForumPosts(overwriteCache = params[1] as Boolean)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: List<ForumPost>?) {
            progress_forum?.visibility = ProgressBar.INVISIBLE
            forum_swipe_refresh?.isRefreshing = false
            if (context != null) {
                if (result != null) {
                    forum_list?.adapter = ForumPostAdapter(context!!, result)
                } else {
                    Toast.makeText(context, getString(R.string.request_failed_other).format("No details"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}