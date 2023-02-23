package de.deftk.openww.android.fragments

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
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
    open val adapter by lazy { OperatingScopeAdapter(this) }

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
                adapter.submitList(emptyList())
                setUIState(UIState.EMPTY)
            }
        }

        return binding.root
    }

    protected fun updateGroups(apiContext: IApiContext) {
        setUIState(UIState.LOADING)
        var scopes: MutableList<IOperatingScope> = apiContext.user.getGroups().filter { scopePredicate(it) }.toMutableList()
        if (scopePredicate(apiContext.user))
            scopes.add(0, apiContext.user)
        scopes = (filter.apply(scopes).filterIsInstance<IOperatingScope>()).toMutableList()
        adapter.submitList(scopes)
        if (scopes.isEmpty()) {
            setUIState(UIState.EMPTY)
        } else {
            setUIState(UIState.READY)
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_options_menu, menu)
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

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.groupsSwipeRefresh.isEnabled = newState.swipeRefreshEnabled
        binding.groupsEmpty.isVisible = newState.showEmptyIndicator
        binding.groupsSwipeRefresh.isRefreshing = newState.refreshing
    }
}

interface IOperatingScopeClickListener {
    fun onOperatingScopeClicked(scope: IOperatingScope)
}