package de.deftk.openlonet.fragments.feature.forum

import de.deftk.lonet.api.implementation.Group
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.IGroup
import de.deftk.lonet.api.model.IOperatingScope
import de.deftk.lonet.api.model.IUser
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.ForumAdapter
import de.deftk.openlonet.feature.AppFeature
import de.deftk.openlonet.fragments.GroupFragment
import de.deftk.openlonet.utils.filter.FilterableAdapter

class ForumGroupFragment : GroupFragment(
    AppFeature.FEATURE_FORUM,
    R.layout.fragment_forum,
    R.id.forum_list,
    R.id.progress_forum,
    R.id.forum_empty,
    R.id.forum_swipe_refresh
) {

    override fun createAdapter(elements: List<IOperatingScope>): FilterableAdapter<Group> {
        return ForumAdapter(requireContext(), elements.filterIsInstance<Group>())
    }

    override fun shouldGroupBeShown(group: IGroup): Boolean {
        return Feature.FORUM.isAvailable(group.effectiveRights)
    }

    override fun shouldUserBeShown(user: IUser): Boolean {
        return false
    }

    override fun onItemClick(operator: IOperatingScope) {
        val action = ForumGroupFragmentDirections.actionForumGroupFragmentToForumPostsFragment(operator.login, operator.name)
        navController.navigate(action)
    }

}