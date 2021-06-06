package de.deftk.openlonet.fragments

import android.os.Bundle
import android.view.View
import android.widget.ListAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.openlonet.viewmodel.UserViewModel

@Deprecated("not compatible with current architecture")
abstract class AbstractListFragment<T>: Fragment() {

    companion object {
        private const val SAVED_LIST_VIEW_STATE = "SAVED_LIST_VIEW_STATE"
    }

    protected val userViewModel: UserViewModel by activityViewModels()
    protected val navController: NavController by lazy { findNavController() }
    protected abstract val dataHolder: Lazy<LiveData<List<T>>>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        retainInstance = true
        getListView().setOnItemClickListener { _, clickedView, position, _ ->
            val item = getListView().getItemAtPosition(position) ?: return@setOnItemClickListener
            @Suppress("UNCHECKED_CAST")
            showDetails(item as T, clickedView)
        }
        getSwipeRefreshLayout()?.setOnRefreshListener {
            getListView().adapter = null
            userViewModel.apiContext.value?.apply {
                startRefreshDataHolder(this)
            }
        }
        dataHolder.value.observe(viewLifecycleOwner) { elements ->
            if (elements != null) {
                updateData(elements)
                if (savedInstanceState?.containsKey(SAVED_LIST_VIEW_STATE) == true) {
                    getListView().onRestoreInstanceState(savedInstanceState.getParcelable(SAVED_LIST_VIEW_STATE))
                    savedInstanceState.remove(SAVED_LIST_VIEW_STATE)
                }
            }
        }

        // try to fill current dataHolder if it is empty
        if (dataHolder.value.value == null) {
            userViewModel.apiContext.value?.apply {
                startRefreshDataHolder(this)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        //outState.putInt(SAVED_LIST_VIEW_STATE, getListView().firstVisiblePosition)
        outState.putParcelable(SAVED_LIST_VIEW_STATE, getListView().onSaveInstanceState())
    }

    protected fun updateData(elements: List<T>?) {
        if (elements != null && elements.isNotEmpty()) {
            getListView().adapter = createAdapter(elements)
            disableLoading(emptyResult = false)
        } else {
            getListView().adapter = null
            disableLoading(emptyResult = true)
        }
        getSwipeRefreshLayout()?.isRefreshing = false
    }

    protected abstract fun startRefreshDataHolder(apiContext: ApiContext)

    protected abstract fun getListView(): ListView

    protected abstract fun getSwipeRefreshLayout(): SwipeRefreshLayout?

    protected abstract fun createAdapter(elements: List<T>): ListAdapter

    protected abstract fun disableLoading(emptyResult: Boolean)

    protected abstract fun showDetails(item: T, view: View)

}