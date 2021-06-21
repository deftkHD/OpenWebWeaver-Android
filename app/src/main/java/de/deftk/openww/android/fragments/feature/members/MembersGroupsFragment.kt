package de.deftk.openww.android.fragments.feature.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.Feature
import de.deftk.openww.android.adapter.recycler.MemberAdapter
import de.deftk.openww.android.databinding.FragmentMembersBinding
import de.deftk.openww.android.viewmodel.UserViewModel

class MembersGroupsFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var binding: FragmentMembersBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMembersBinding.inflate(inflater, container, false)

        val adapter = MemberAdapter()
        binding.memberList.adapter = adapter
        binding.memberList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.membersSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                updateGroups(adapter, apiContext)
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                updateGroups(adapter, apiContext)
            } else {
                binding.membersEmpty.isVisible = false
                adapter.submitList(emptyList())
                binding.progressMembers.isVisible = true
            }
        }

        return binding.root
    }

    private fun updateGroups(adapter: MemberAdapter, apiContext: ApiContext) {
        val groups = apiContext.getUser().getGroups().filter { Feature.MEMBERS.isAvailable(it.effectiveRights) }
        adapter.submitList(groups)
        binding.membersEmpty.isVisible = groups.isEmpty()
        binding.progressMembers.isVisible = false
        binding.membersSwipeRefresh.isRefreshing = false
    }

}