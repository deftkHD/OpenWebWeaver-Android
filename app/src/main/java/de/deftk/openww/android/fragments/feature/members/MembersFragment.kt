package de.deftk.openww.android.fragments.feature.members

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.activities.MainActivity
import de.deftk.openww.android.adapter.recycler.MemberAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentMembersBinding
import de.deftk.openww.android.filter.ScopeFilter
import de.deftk.openww.android.fragments.ContextualFragment
import de.deftk.openww.android.utils.ISearchProvider
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.GroupViewModel
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IGroup

class MembersFragment : ContextualFragment(true), ISearchProvider {

    private val args: MembersFragmentArgs by navArgs()
    private val groupViewModel: GroupViewModel by activityViewModels()

    private lateinit var binding: FragmentMembersBinding
    private lateinit var searchView: SearchView

    private var group: IGroup? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMembersBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        (requireActivity() as? MainActivity?)?.searchProvider = this

        val adapter = MemberAdapter()
        binding.memberList.adapter = adapter
        binding.memberList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.membersSwipeRefresh.setOnRefreshListener {
            loginViewModel.apiContext.value?.also { apiContext ->
                groupViewModel.loadMembers(group!!, false, apiContext)
                setUIState(UIState.LOADING)
            }
        }

        loginViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                val refreshedGroup = apiContext.user.getGroups().firstOrNull { it.login == args.groupId }
                if (refreshedGroup == null) {
                    Reporter.reportException(R.string.error_scope_not_found, args.groupId, requireContext())
                    navController.popBackStack()
                    return@observe
                }
                if (!Feature.MEMBERS.isAvailable(refreshedGroup.effectiveRights)) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }
                if (group != null)
                    groupViewModel.getFilteredGroupMembers(group!!).removeObservers(viewLifecycleOwner)
                group = refreshedGroup
                groupViewModel.getFilteredGroupMembers(group!!).observe(viewLifecycleOwner) { response ->
                    if (response is Response.Success) {
                        adapter.submitList(response.value)
                        setUIState(if (response.value.isEmpty()) UIState.EMPTY else UIState.READY)
                    } else if (response is Response.Failure) {
                        setUIState(UIState.ERROR)
                        Reporter.reportException(R.string.error_get_members_failed, response.exception, requireContext())
                    }
                }
                if (groupViewModel.getAllGroupMembers(group!!).value == null) {
                    groupViewModel.loadMembers(group!!, false, apiContext)
                    setUIState(UIState.LOADING)
                }
            } else {
                adapter.submitList(emptyList())
                binding.membersEmpty.isVisible = false
                setUIState(UIState.DISABLED)
            }
        }

        registerForContextMenu(binding.memberList)
        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_options_menu, menu)
        val searchItem = menu.findItem(R.id.list_options_item_search)
        searchView = searchItem.actionView as SearchView
        searchView.setQuery(groupViewModel.filter.value?.smartSearchCriteria?.value, false) // restore recent search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filter = ScopeFilter()
                filter.smartSearchCriteria.value = newText
                groupViewModel.filter.value = filter
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

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            val member = (binding.memberList.adapter as MemberAdapter).getItem(menuInfo.position)
            val apiContext = loginViewModel.apiContext.value ?: return
            if (member.login != apiContext.user.login) {
                requireActivity().menuInflater.inflate(R.menu.member_context_menu, menu)
                menu.findItem(R.id.member_context_item_open_chat).isVisible = Feature.MESSENGER.isAvailable(apiContext.user.effectiveRights)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.memberList.adapter as MemberAdapter
        return when (item.itemId) {
            R.id.member_context_item_open_chat -> {
                val member = adapter.getItem(menuInfo.position)
                navController.navigate(MembersFragmentDirections.actionMembersFragmentToMessengerChatFragment(member.login, member.name))
                true
            }
            R.id.member_context_item_write_mail -> {
                val member = adapter.getItem(menuInfo.position)
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(member.login)}"))
                startActivity(Intent.createChooser(intent, getString(R.string.send_mail)))
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.memberList.isEnabled = newState.listEnabled
        binding.membersEmpty.isVisible = newState.showEmptyIndicator
        binding.membersSwipeRefresh.isEnabled = newState.swipeRefreshEnabled
        binding.membersSwipeRefresh.isRefreshing = newState.refreshing
    }
}