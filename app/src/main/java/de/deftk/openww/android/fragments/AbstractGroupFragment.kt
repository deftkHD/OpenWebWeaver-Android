package de.deftk.openww.android.fragments

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.OperatingScopeAdapter
import de.deftk.openww.android.databinding.FragmentGroupsBinding
import de.deftk.openww.android.filter.ScopeFilter
import de.deftk.openww.android.filter.ScopeOrder
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IOperatingScope

abstract class AbstractGroupFragment : Fragment(), IOperatingScopeClickListener {

    protected lateinit var binding: FragmentGroupsBinding

    protected val userViewModel: UserViewModel by activityViewModels()
    protected val navController by lazy { findNavController() }

    abstract val scopePredicate: (T : IOperatingScope) -> Boolean
    open val adapter: ListAdapter<IOperatingScope, RecyclerView.ViewHolder> = OperatingScopeAdapter(this)

    private var filter = ScopeFilter(ScopeOrder.ByOperatorDefault)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentGroupsBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
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

        setHasOptionsMenu(true)
        return binding.root
    }

    protected fun updateGroups(apiContext: ApiContext) {
        var scopes: MutableList<IOperatingScope> = apiContext.user.getGroups().filter { scopePredicate(it) }.toMutableList()
        if (scopePredicate(apiContext.user))
            scopes.add(0, apiContext.user)
        scopes = (filter.apply(scopes) as List<IOperatingScope>).toMutableList()
        adapter.submitList(scopes)
        binding.groupsEmpty.isVisible = scopes.isEmpty()
        binding.progressGroups.isVisible = false
        binding.groupsSwipeRefresh.isRefreshing = false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setQuery(filter.smartSearchCriteria.value, false) // restore recent search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filter = ScopeFilter(ScopeOrder.ByOperatorDefault)
                filter.smartSearchCriteria.value = newText
                this@AbstractGroupFragment.filter = filter
                userViewModel.apiContext.value?.also { apiContext ->
                    updateGroups(apiContext)
                }
                return false
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

}

interface IOperatingScopeClickListener {
    fun onOperatingScopeClicked(scope: IOperatingScope)
}