package de.deftk.openlonet.fragments

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.implementation.Group
import de.deftk.lonet.api.implementation.OperatingScope
import de.deftk.lonet.api.implementation.User
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.abstract.FeatureFragment
import de.deftk.openlonet.abstract.IBackHandler
import de.deftk.openlonet.feature.AppFeature
import de.deftk.openlonet.utils.filter.FilterableAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A fragment containing a list of groups (or other objects e.g. the user) capable of performing
 * operations that belong to the given AppFeature
 * @param layoutId: Id of the layout file of the fragment (R.layout.?)
 * @param groupListId: Id of the ListView in which the groups will be shown
 * @param swipeRefreshLayoutId: Id of the SwipeRefreshLayout that will be used to refresh the groups
 * @param progressBarId: Id of the ProgressBar indicating a running action
 * @param emptyLabelId: Id of the TextView shown when no entries are available
 *
 */
abstract class GroupFragment(
    feature: AppFeature,
    private val layoutId: Int,
    private val groupListId: Int,
    private val swipeRefreshLayoutId: Int,
    private val progressBarId: Int,
    private val emptyLabelId: Int
) : FeatureFragment(feature), IBackHandler {

    protected lateinit var groupList: ListView
    protected lateinit var swipeRefreshLayout: SwipeRefreshLayout
    protected lateinit var progressBar: ProgressBar
    protected lateinit var emptyLabel: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        val view = inflater.inflate(layoutId, container, false)
        groupList = view.findViewById(groupListId)
        progressBar = view.findViewById(progressBarId)
        emptyLabel = view.findViewById(emptyLabelId)
        swipeRefreshLayout = view.findViewById(swipeRefreshLayoutId)

        // setup listeners
        swipeRefreshLayout.setOnRefreshListener {
            reloadGroups()
        }
        groupList.setOnItemClickListener { _, _, position, _ ->
            val item = groupList.getItemAtPosition(position)
            if (item is OperatingScope)
                onItemClick(item)
        }

        // load content
        reloadGroups()

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

    protected open fun reloadGroups() {
        groupList.adapter = null
        emptyLabel.visibility = TextView.GONE
        CoroutineScope(Dispatchers.IO).launch {
            loadGroups()
        }
    }

    protected open suspend fun loadGroups() {
        try {
            val groups = mutableListOf<OperatingScope>()
            groups.addAll(AuthStore.getApiUser().getGroups().filter { shouldGroupBeShown(it) })
            if (shouldUserBeShown(AuthStore.getApiUser()))
                groups.add(0, AuthStore.getApiUser())
            withContext(Dispatchers.Main) {
                groupList.adapter = createAdapter(groups)
                emptyLabel.isVisible = groups.isEmpty()
                swipeRefreshLayout.isRefreshing = false
                progressBar.visibility = ProgressBar.INVISIBLE
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                swipeRefreshLayout.isRefreshing = false
                progressBar.visibility = ProgressBar.INVISIBLE
                Toast.makeText(
                    context,
                    getString(R.string.request_failed_other).format(e.message ?: e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    abstract fun createAdapter(groups: List<OperatingScope>): FilterableAdapter<*>

    abstract fun shouldGroupBeShown(group: Group): Boolean

    abstract fun shouldUserBeShown(user: User): Boolean

    abstract fun onItemClick(operator: OperatingScope)

    override fun onBackPressed() = false
}