package de.deftk.lonet.mobile.fragments

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.Member
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

    private var currentForum: Member? = null
    private lateinit var list: ListView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progress: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_forum, container, false)
        list = view.findViewById(R.id.forum_list)
        progress = view.findViewById(R.id.progress_forum)
        swipeRefresh = view.findViewById(R.id.forum_swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            navigate(currentForum, true)
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val item = list.getItemAtPosition(position)
            if (item is Member) {
                navigate(item, false)
            } else {
                item as ForumPost

                val intent = Intent(context, ForumPostActivity::class.java)
                intent.putExtra(ForumPostActivity.EXTRA_LOGIN, currentForum!!.login)
                intent.putExtra(ForumPostActivity.EXTRA_POST_ID, item.id)
                startActivity(intent)
            }
        }
        navigate(null, false)
        return view
    }

    private fun navigate(forum: Member?, overwriteCache: Boolean) {
        currentForum = forum
        if (forum == null) {
            list.adapter = ForumAdapter(context ?: error("Oops, no context?"),
                AuthStore.appUser.memberships.filter { Feature.FORUM.isAvailable(it.permissions) }) //TODO not sure if i should include user itself; filter by permission (but it should work...)
            swipeRefresh.isRefreshing = false
            progress.visibility = ProgressBar.INVISIBLE
        } else {
            list.adapter = null
            EntryLoader().execute(forum, overwriteCache)
        }
    }

    override fun onBackPressed(): Boolean {
        if (currentForum != null) {
            navigate(null, false)
            return true
        }
        return false
    }

    private inner class EntryLoader : AsyncTask<Any, Void, List<ForumPost>>() {

        override fun doInBackground(vararg params: Any): List<ForumPost> {
            return (params[0] as Member).getForumPosts(
                AuthStore.appUser.sessionId,
                overwriteCache = params[1] as Boolean
            )
        }

        override fun onPostExecute(result: List<ForumPost>) {
            forum_list?.adapter = ForumPostAdapter(context ?: error("Oops, no context?"), result)
            progress_forum?.visibility = ProgressBar.INVISIBLE
            forum_swipe_refresh?.isRefreshing = false
        }
    }

}