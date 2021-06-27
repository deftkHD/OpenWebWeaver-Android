package de.deftk.openww.android.fragments.feature.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.databinding.FragmentMembersBinding
import de.deftk.openww.android.fragments.AbstractGroupFragment
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IOperatingScope

class MembersGroupsFragment : AbstractGroupFragment() {

    private lateinit var binding: FragmentMembersBinding

    override val scopePredicate: (T: IOperatingScope) -> Boolean = { Feature.MEMBERS.isAvailable(it.effectiveRights) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMembersBinding.inflate(inflater, container, false)
        binding.memberList.adapter = adapter
        binding.memberList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        registerSwipeRefresh(binding.membersSwipeRefresh)
        return binding.root
    }

    override fun setUI(empty: Boolean, loading: Boolean, refreshing: Boolean) {
        binding.membersEmpty.isVisible = empty
        binding.progressMembers.isVisible = loading
        binding.membersSwipeRefresh.isRefreshing = refreshing
    }

    override fun onOperatingScopeClicked(scope: IOperatingScope) {
        navController.navigate(MembersGroupsFragmentDirections.actionMembersGroupFragmentToMembersFragment(scope.login, scope.name))
    }
}