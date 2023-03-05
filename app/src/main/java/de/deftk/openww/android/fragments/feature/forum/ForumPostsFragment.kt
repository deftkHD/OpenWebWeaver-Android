package de.deftk.openww.android.fragments.feature.forum

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ActionModeAdapter
import de.deftk.openww.android.adapter.recycler.ForumPostAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentForumPostsBinding
import de.deftk.openww.android.filter.ForumPostFilter
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.utils.ISearchProvider
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.ForumViewModel
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.forum.IForumPost

class ForumPostsFragment : ActionModeFragment<IForumPost, ForumPostAdapter.ForumPostViewHolder>(R.menu.forum_actionmode_menu), ISearchProvider {

    //TODO needs recode to remove title from navargs and being able to be called by deeplink

    private val args: ForumPostsFragmentArgs by navArgs()
    private val forumViewModel: ForumViewModel by activityViewModels()

    private lateinit var binding: FragmentForumPostsBinding
    private lateinit var searchView: SearchView

    private var group: IGroup? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentForumPostsBinding.inflate(inflater, container, false)

        binding.forumSwipeRefresh.setOnRefreshListener {
            loginViewModel.apiContext.value?.also { apiContext ->
                forumViewModel.loadForumPosts(group!!, null, apiContext)
                setUIState(UIState.LOADING)
            }
        }

        forumViewModel.deleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                forumViewModel.resetDeleteResponse() // mark as handled

            if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            } else if (response is Response.Success) {
                setUIState(UIState.READY)
            }
        }

        forumViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                forumViewModel.resetBatchDeleteResponse()

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
            } else {
                setUIState(UIState.READY)
                actionMode?.finish()
            }
        }

        loginViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                val newGroup = apiContext.user.getGroups().firstOrNull { it.login == args.groupId }
                if (newGroup == null) {
                    Reporter.reportException(R.string.error_scope_not_found, args.groupId, requireContext())
                    navController.popBackStack()
                    return@observe
                }
                if (!Feature.FORUM.isAvailable(newGroup.effectiveRights)) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }

                if (group != null) {
                    forumViewModel.getFilteredForumPosts(group!!).removeObservers(viewLifecycleOwner)
                    group = newGroup
                    (adapter as ForumPostAdapter).group = group!!
                } else {
                    group = newGroup
                }
                binding.forumList.adapter = adapter
                binding.forumList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
                forumViewModel.getFilteredForumPosts(group!!).observe(viewLifecycleOwner) { response ->
                    if (response is Response.Success) {
                        val posts = forumViewModel.filterRootPosts(response.value)
                        adapter.submitList(posts)
                        setUIState(if (posts.isEmpty()) UIState.EMPTY else UIState.READY)
                    } else if (response is Response.Failure) {
                        setUIState(UIState.ERROR)
                        Reporter.reportException(R.string.error_get_posts_failed, response.exception, requireContext())
                    }
                    binding.forumSwipeRefresh.isRefreshing = false
                }

                if (forumViewModel.getAllForumPosts(group!!).value == null) {
                    forumViewModel.loadForumPosts(group!!, null, apiContext)
                    setUIState(UIState.LOADING)
                }
            } else {
                adapter.submitList(emptyList())
                setUIState(UIState.DISABLED)
            }
        }

        registerForContextMenu(binding.forumList)
        return binding.root
    }

    override fun createAdapter(): ActionModeAdapter<IForumPost, ForumPostAdapter.ForumPostViewHolder> {
        return ForumPostAdapter(group!!, this)
    }

    override fun onItemClick(view: View, viewHolder: ForumPostAdapter.ForumPostViewHolder) {
        navController.navigate(ForumPostsFragmentDirections.actionForumPostsFragmentToForumPostFragment(viewHolder.binding.group!!.login, viewHolder.binding.post!!.id, getString(R.string.see_post), null))
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val canModify = adapter.selectedItems.all { it.binding.group!!.effectiveRights.contains(Permission.FORUM_WRITE) || it.binding.group!!.effectiveRights.contains(Permission.FORUM_ADMIN) }
        menu.findItem(R.id.forum_action_item_delete).isEnabled = canModify
        return super.onPrepareActionMode(mode, menu)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.forum_action_item_delete -> {
                loginViewModel.apiContext.value?.also { apiContext ->
                    forumViewModel.batchDelete(adapter.selectedItems.map { it.binding.post!! }, group!!, apiContext)
                    setUIState(UIState.LOADING)
                }
            }
            else -> return false
        }
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (group!!.effectiveRights.contains(Permission.FORUM_WRITE) || group!!.effectiveRights.contains(Permission.FORUM_ADMIN)) {
            requireActivity().menuInflater.inflate(R.menu.forum_context_menu, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.forumList.adapter as ForumPostAdapter
        when (item.itemId) {
            R.id.forum_context_item_delete -> {
                val comment = adapter.getItem(menuInfo.position)
                loginViewModel.apiContext.value?.also { apiContext ->
                    forumViewModel.deletePost(comment, null, group!!, apiContext)
                    setUIState(UIState.LOADING)
                }
            }
            else -> super.onContextItemSelected(item)
        }
        return true
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_options_menu, menu)
        val searchItem = menu.findItem(R.id.list_options_item_search)
        searchView = searchItem.actionView as SearchView
        searchView.setQuery(forumViewModel.filter.value?.smartSearchCriteria?.value, false) // restore recent search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filter = ForumPostFilter()
                filter.smartSearchCriteria.value = newText
                forumViewModel.filter.value = filter
                return true
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
        binding.forumSwipeRefresh.isEnabled = newState.swipeRefreshEnabled
        binding.forumSwipeRefresh.isRefreshing = newState.refreshing
        binding.forumList.isEnabled = newState.listEnabled
        binding.forumEmpty.isVisible = newState.showEmptyIndicator
    }
}