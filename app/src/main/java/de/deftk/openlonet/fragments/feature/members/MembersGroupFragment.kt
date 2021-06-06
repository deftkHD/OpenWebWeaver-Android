package de.deftk.openlonet.fragments.feature.members

import de.deftk.lonet.api.model.*
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.MemberAdapter
import de.deftk.openlonet.feature.AppFeature
import de.deftk.openlonet.fragments.GroupFragment
import de.deftk.openlonet.utils.filter.FilterableAdapter

/**
 * Fragment holding a list of all groups accessible by the user
 */
class MembersGroupFragment : GroupFragment(
    AppFeature.FEATURE_MEMBERS,
    R.layout.fragment_member_groups,
    R.id.members_list,
    R.id.progress_members,
    R.id.members_empty,
    R.id.members_swipe_refresh
) {

    override fun createAdapter(elements: List<IOperatingScope>): FilterableAdapter<IScope> {
        return MemberAdapter(requireContext(), elements)
    }

    override fun shouldGroupBeShown(group: IGroup): Boolean {
        return Feature.MEMBERS.isAvailable(group.effectiveRights)
    }

    override fun shouldUserBeShown(user: IUser): Boolean {
        return false
    }

    override fun onItemClick(operator: IOperatingScope) {
        val action = MembersGroupFragmentDirections.actionMembersGroupFragmentToMembersFragment(operator.login, operator.name)
        navController.navigate(action)
    }

}