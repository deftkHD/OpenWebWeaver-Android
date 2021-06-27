package de.deftk.openww.android.fragments.feature.forum

import de.deftk.openww.android.fragments.AbstractGroupFragment
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IOperatingScope

class ForumGroupFragment : AbstractGroupFragment() {

    override val scopePredicate: (T: IOperatingScope) -> Boolean = { Feature.FORUM.isAvailable(it.effectiveRights) }

    override fun onOperatingScopeClicked(scope: IOperatingScope) {
        navController.navigate(ForumGroupFragmentDirections.actionForumGroupFragmentToForumPostsFragment(scope.login, scope.name))
    }

}