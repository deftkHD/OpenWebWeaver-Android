package de.deftk.openww.android.fragments.feature.contacts

import de.deftk.openww.android.fragments.AbstractGroupFragment
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IOperatingScope

class ContactsGroupFragment : AbstractGroupFragment() {

    override val scopePredicate: (T: IOperatingScope) -> Boolean = { Feature.ADDRESSES.isAvailable(it.effectiveRights) }

    override fun onOperatingScopeClicked(scope: IOperatingScope) {
        navController.navigate(ContactsGroupFragmentDirections.actionContactsGroupFragmentToContactsFragment(scope.login, scope.name))
    }
}