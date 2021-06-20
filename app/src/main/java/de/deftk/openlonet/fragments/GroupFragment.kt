package de.deftk.openlonet.fragments

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.lonet.api.implementation.OperatingScope
import de.deftk.lonet.api.model.IGroup
import de.deftk.lonet.api.model.IOperatingScope
import de.deftk.lonet.api.model.IUser
import de.deftk.openlonet.R
import de.deftk.openlonet.feature.AppFeature

/**
 * A fragment containing a list of groups (or other objects e.g. the user) capable of performing
 * operations that belong to the given AppFeature
 * @param layoutId: Id of the layout file of the fragment (R.layout.?)
 * @param groupListId: Id of the ListView in which the groups will be shown
 * @param progressBarId: Id of the ProgressBar indicating a running action
 * @param emptyLabelId: Id of the TextView shown when no entries are available
 * @param swipeRefreshLayoutId: Id the the SwipeRefreshLayout or null if none is used
 */
abstract class GroupFragment(
    val feature: AppFeature,
    private val layoutId: Int,
    private val groupListId: Int,
    private val progressBarId: Int,
    private val emptyLabelId: Int,
    private val swipeRefreshLayoutId: Int?
) : AbstractListFragment<IOperatingScope>() {

    //TODO fix swipe refresh

    private lateinit var groupList: ListView
    protected lateinit var progressBar: ProgressBar
    protected lateinit var emptyLabel: TextView
    private  var swipeRefreshLayout: SwipeRefreshLayout? = null

    override val dataHolder: Lazy<LiveData<List<IOperatingScope>>> = lazy { userViewModel.apiContext.map { filterScopes(it) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        val view = inflater.inflate(layoutId, container, false)
        groupList = view.findViewById(groupListId)
        progressBar = view.findViewById(progressBarId)
        emptyLabel = view.findViewById(emptyLabelId)
        if (swipeRefreshLayoutId != null)
            swipeRefreshLayout = view.findViewById(swipeRefreshLayoutId)

        groupList.setOnItemClickListener { _, _, position, _ ->
            val item = groupList.getItemAtPosition(position)
            if (item is OperatingScope)
                onItemClick(item)
        }

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                (groupList.adapter as Filterable).filter.filter(newText)
                return false
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun disableLoading(emptyResult: Boolean) {
        emptyLabel.isVisible = emptyResult
        progressBar.visibility = ProgressBar.INVISIBLE
    }

    override fun showDetails(item: IOperatingScope, view: View) {
        onItemClick(item)
    }

    private fun filterScopes(apiContext: ApiContext?): List<IOperatingScope> {
        if (apiContext == null)
            return emptyList()
        val groups = mutableListOf<OperatingScope>()
        if (shouldUserBeShown(apiContext.getUser()))
            groups.add(0, apiContext.getUser())
        groups.addAll(apiContext.getUser().getGroups().filter { shouldGroupBeShown(it) })
        return groups
    }

    override fun startRefreshDataHolder(apiContext: ApiContext) {
        // groups don't need to be refreshed normally (or rather say can't be refreshed except
        // by sending a new login request)
    }

    override fun getListView(): ListView = groupList

    override fun getSwipeRefreshLayout(): SwipeRefreshLayout? = swipeRefreshLayout

    abstract fun shouldGroupBeShown(group: IGroup): Boolean

    abstract fun shouldUserBeShown(user: IUser): Boolean

    abstract fun onItemClick(operator: IOperatingScope)
}