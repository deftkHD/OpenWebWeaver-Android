package de.deftk.lonet.mobile.fragments

import android.content.Intent
import android.widget.ArrayAdapter
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.Group
import de.deftk.lonet.api.model.User
import de.deftk.lonet.api.model.abstract.AbstractOperator
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.IBackHandler
import de.deftk.lonet.mobile.activities.feature.forum.ForumPostsActivity
import de.deftk.lonet.mobile.adapter.ForumAdapter
import de.deftk.lonet.mobile.feature.AppFeature

class ForumGroupFragment : GroupFragment(AppFeature.FEATURE_FORUM, R.layout.fragment_forum, R.id.forum_list, R.id.forum_swipe_refresh, R.id.progress_forum, R.id.forum_empty), IBackHandler {

    override fun createAdapter(groups: List<AbstractOperator>): ArrayAdapter<*> {
        return ForumAdapter(requireContext(), groups.filterIsInstance<Group>())
    }

    override fun shouldGroupBeShown(group: Group): Boolean {
        return Feature.FORUM.isAvailable(group.effectiveRights)
    }

    override fun shouldUserBeShown(user: User): Boolean {
        return false
    }

    override fun onItemClick(operator: AbstractOperator) {
        if (operator is Group) {
            val intent = Intent(requireContext(), ForumPostsActivity::class.java)
            intent.putExtra(ForumPostsActivity.EXTRA_GROUP, operator)
            startActivity(intent)
        }
    }

    override fun getTitle(): String {
        return getString(R.string.forum)
    }

}