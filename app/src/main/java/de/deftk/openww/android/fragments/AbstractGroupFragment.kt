package de.deftk.openww.android.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.openww.android.adapter.recycler.OperatingScopeAdapter
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.IUser

abstract class AbstractGroupFragment : Fragment(), IOperatingScopeClickListener {

    protected val userViewModel: UserViewModel by activityViewModels()
    protected val navController by lazy { findNavController() }

    abstract val scopePredicate: (T : IOperatingScope) -> Boolean
    open val adapter: ListAdapter<IOperatingScope, RecyclerView.ViewHolder> = OperatingScopeAdapter(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                updateGroups(apiContext)
            } else {
                setUI(empty = false, loading = true, refreshing = false)
                adapter.submitList(emptyList())
            }
        }
    }

    protected fun updateGroups(apiContext: ApiContext) {
        var scopes: MutableList<IOperatingScope> = apiContext.getUser().getGroups().filter { scopePredicate(it) }.toMutableList()
        if (scopePredicate(apiContext.getUser()))
            scopes.add(0, apiContext.getUser())
        scopes = scopes.sortedWith(compareBy ({ it !is IUser }, { it.name })).toMutableList()
        adapter.submitList(scopes)
        setUI(scopes.isEmpty(), loading = false, refreshing = false)
    }

    protected fun registerSwipeRefresh(swipeRefreshLayout: SwipeRefreshLayout) {
        swipeRefreshLayout.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                updateGroups(apiContext)
            }
        }
    }

    abstract fun setUI(empty: Boolean, loading: Boolean, refreshing: Boolean)

}

interface IOperatingScopeClickListener {
    fun onOperatingScopeClicked(scope: IOperatingScope)
}