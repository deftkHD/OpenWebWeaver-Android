package de.deftk.openww.android.fragments.feature.members

import de.deftk.openww.android.fragments.AbstractGroupFragment
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IOperatingScope

class MembersGroupsFragment : AbstractGroupFragment() {

    override val scopePredicate: (T: IOperatingScope) -> Boolean = { Feature.MEMBERS.isAvailable(it.effectiveRights) }

    override fun onOperatingScopeClicked(scope: IOperatingScope) {
        navController.navigate(MembersGroupsFragmentDirections.actionMembersGroupFragmentToMembersFragment(scope.login, scope.name))
    }
}