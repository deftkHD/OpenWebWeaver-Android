package de.deftk.openww.android.fragments.devtools.permission

import de.deftk.openww.android.fragments.AbstractGroupFragment
import de.deftk.openww.api.model.IOperatingScope

class PermissionScopesFragment : AbstractGroupFragment() {

    override val scopePredicate: (T: IOperatingScope) -> Boolean = { true }

    override fun onOperatingScopeClicked(scope: IOperatingScope) {
        navController.navigate(PermissionScopesFragmentDirections.actionPermissionScopesFragmentToPermissionsFragment(scope.login))
    }

}