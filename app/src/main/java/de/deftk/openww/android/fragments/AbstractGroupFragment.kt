package de.deftk.openww.android.fragments

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.R
import de.deftk.openww.android.activities.MainActivity
import de.deftk.openww.android.adapter.recycler.OperatingScopeAdapter
import de.deftk.openww.android.databinding.FragmentGroupsBinding
import de.deftk.openww.android.filter.ScopeFilter
import de.deftk.openww.android.filter.ScopeOrder
import de.deftk.openww.android.utils.ISearchProvider
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IOperatingScope

abstract class AbstractGroupFragment : AbstractFragment(true), IOperatingScopeClickListener, ISearchProvider {

    protected lateinit var binding: FragmentGroupsBinding
    private lateinit var searchView: SearchView

    protected val userViewModel: UserViewModel by activityViewModels()
    protected val navController by lazy { findNavController() }

    abstract val scopePredicate: (T : IOperatingScope) -> Boolean
    open val adapter: ListAdapter<IOperatingScope, RecyclerView.ViewHolder> by lazy { OperatingScopeAdapter(this) }

    private var filter = ScopeFilter(ScopeOrder.ByOperatorDefault)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentGroupsBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        (requireActivity() as? MainActivity?)?.searchProvider = this
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
                enableUI(false)
                binding.groupsSwipeRefresh.isRefreshing = false
                adapter.submitList(emptyList())
            }
        }

        setHasOptionsMenu(true)
        return binding.root
    }

    protected fun updateGroups(apiContext: IApiContext) {
        var scopes: MutableList<IOperatingScope> = apiContext.user.getGroups().filter { scopePredicate(it) }.toMutableList()
        if (scopePredicate(apiContext.user))
            scopes.add(0, apiContext.user)
        scopes = (filter.apply(scopes) as List<IOperatingScope>).toMutableList()
        adapter.submitList(scopes)
        binding.groupsEmpty.isVisible = scopes.isEmpty()
        enableUI(true)
        binding.groupsSwipeRefresh.isRefreshing = false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.list_options_menu, menu)
        val searchItem = menu.findItem(R.id.list_options_item_search)
        searchView = searchItem.actionView as SearchView
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

    override fun onSearchBackPressed(): Boolean {
        return if (searchView.isIconified) {
            false
        } else {
            searchView.isIconified = true
            searchView.setQuery(null, true)
            true
        }
    }

    override fun onUIStateChanged(enabled: Boolean) {
        binding.groupsSwipeRefresh.isEnabled = enabled
    }
}

interface IOperatingScopeClickListener {
    fun onOperatingScopeClicked(scope: IOperatingScope)
}