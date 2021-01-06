package de.deftk.openlonet.fragments

import android.content.Intent
import de.deftk.lonet.api.implementation.Group
import de.deftk.lonet.api.implementation.OperatingScope
import de.deftk.lonet.api.implementation.User
import de.deftk.lonet.api.model.Feature
import de.deftk.openlonet.R
import de.deftk.openlonet.abstract.IBackHandler
import de.deftk.openlonet.activities.feature.forum.ForumPostsActivity
import de.deftk.openlonet.adapter.ForumAdapter
import de.deftk.openlonet.feature.AppFeature
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.putJsonExtra

class ForumGroupFragment : GroupFragment(AppFeature.FEATURE_FORUM, R.layout.fragment_forum, R.id.forum_list, R.id.forum_swipe_refresh, R.id.progress_forum, R.id.forum_empty), IBackHandler {

    override fun createAdapter(groups: List<OperatingScope>): FilterableAdapter<Group> {
        return ForumAdapter(requireContext(), groups.filterIsInstance<Group>())
    }

    override fun shouldGroupBeShown(group: Group): Boolean {
        return Feature.FORUM.isAvailable(group.effectiveRights)
    }

    override fun shouldUserBeShown(user: User): Boolean {
        return false
    }

    override fun onItemClick(operator: OperatingScope) {
        if (operator is Group) {
            val intent = Intent(requireContext(), ForumPostsActivity::class.java)
            intent.putJsonExtra(ForumPostsActivity.EXTRA_GROUP, operator)
            startActivity(intent)
        }
    }

    override fun getTitle(): String {
        return getString(R.string.forum)
    }

}