package de.deftk.openww.android.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.adapter.recycler.OperatingScopeAdapter
import de.deftk.openww.android.databinding.FragmentGroupsBinding
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.IUser

abstract class AbstractGroupFragment : Fragment(), IOperatingScopeClickListener {

    protected lateinit var binding: FragmentGroupsBinding

    protected val userViewModel: UserViewModel by activityViewModels()
    protected val navController by lazy { findNavController() }

    abstract val scopePredicate: (T : IOperatingScope) -> Boolean
    open val adapter: ListAdapter<IOperatingScope, RecyclerView.ViewHolder> = OperatingScopeAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentGroupsBinding.inflate(inflater, container, false)
        binding.groupList.adapter = adapter
        binding.groupList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.groupsSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                updateGroups(apiContext)
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                updateGroups(apiContext)
            } else {
                binding.groupsEmpty.isVisible = false
                binding.progressGroups.isVisible = true
                binding.groupsSwipeRefresh.isRefreshing = false
                adapter.submitList(emptyList())
            }
        }

        return binding.root
    }

    protected fun updateGroups(apiContext: ApiContext) {
        var scopes: MutableList<IOperatingScope> = apiContext.getUser().getGroups().filter { scopePredicate(it) }.toMutableList()
        if (scopePredicate(apiContext.getUser()))
            scopes.add(0, apiContext.getUser())
        scopes = scopes.sortedWith(compareBy ({ it !is IUser }, { it.name })).toMutableList()
        adapter.submitList(scopes)
        binding.groupsEmpty.isVisible = scopes.isEmpty()
        binding.progressGroups.isVisible = false
        binding.groupsSwipeRefresh.isRefreshing = false
    }

}

interface IOperatingScopeClickListener {
    fun onOperatingScopeClicked(scope: IOperatingScope)
}