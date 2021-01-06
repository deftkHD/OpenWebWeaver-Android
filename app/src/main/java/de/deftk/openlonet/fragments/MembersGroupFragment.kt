package de.deftk.openlonet.fragments

import android.content.Intent
import de.deftk.lonet.api.implementation.Group
import de.deftk.lonet.api.implementation.OperatingScope
import de.deftk.lonet.api.implementation.User
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.IScope
import de.deftk.openlonet.R
import de.deftk.openlonet.abstract.IBackHandler
import de.deftk.openlonet.activities.feature.MembersActivity
import de.deftk.openlonet.adapter.MemberAdapter
import de.deftk.openlonet.feature.AppFeature
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.putJsonExtra

/**
 * Fragment holding a list of all groups accessible by the user
 */
class MembersGroupFragment : GroupFragment(AppFeature.FEATURE_MEMBERS, R.layout.fragment_members, R.id.members_list, R.id.members_swipe_refresh, R.id.progress_members, R.id.members_empty), IBackHandler {

    override fun createAdapter(groups: List<OperatingScope>): FilterableAdapter<IScope> {
        return MemberAdapter(requireContext(), groups)
    }

    override fun shouldGroupBeShown(group: Group): Boolean {
        return Feature.MEMBERS.isAvailable(group.effectiveRights)
    }

    override fun shouldUserBeShown(user: User): Boolean {
        return false
    }

    override fun onItemClick(operator: OperatingScope) {
        if (operator is Group) {
            val intent = Intent(requireContext(), MembersActivity::class.java)
            intent.putJsonExtra(MembersActivity.EXTRA_GROUP, operator)
            startActivity(intent)
        }
    }

    override fun getTitle(): String {
        return getString(R.string.members)
    }

}