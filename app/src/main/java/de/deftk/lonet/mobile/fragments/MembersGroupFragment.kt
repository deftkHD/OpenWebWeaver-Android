package de.deftk.lonet.mobile.fragments

import android.content.Intent
import android.widget.ArrayAdapter
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.Group
import de.deftk.lonet.api.model.User
import de.deftk.lonet.api.model.abstract.AbstractOperator
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.IBackHandler
import de.deftk.lonet.mobile.activities.feature.MembersActivity
import de.deftk.lonet.mobile.adapter.MemberAdapter
import de.deftk.lonet.mobile.feature.AppFeature

/**
 * Fragment holding a list of all groups accessible by the user
 */
class MembersGroupFragment : GroupFragment(AppFeature.FEATURE_MEMBERS, R.layout.fragment_members, R.id.members_list, R.id.members_swipe_refresh, R.id.progress_members, R.id.members_empty), IBackHandler {

    override fun createAdapter(groups: List<AbstractOperator>): ArrayAdapter<*> {
        return MemberAdapter(requireContext(), groups)
    }

    override fun shouldGroupBeShown(group: Group): Boolean {
        return Feature.MEMBERS.isAvailable(group.effectiveRights)
    }

    override fun shouldUserBeShown(user: User): Boolean {
        return false
    }

    override fun onItemClick(operator: AbstractOperator) {
        if (operator is Group) {
            val intent = Intent(requireContext(), MembersActivity::class.java)
            intent.putExtra(MembersActivity.EXTRA_GROUP, operator)
            startActivity(intent)
        }
    }

    override fun getTitle(): String {
        return getString(R.string.members)
    }

}