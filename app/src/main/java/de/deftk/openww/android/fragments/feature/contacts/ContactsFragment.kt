package de.deftk.openww.android.fragments.feature.contacts

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
import de.deftk.openww.android.adapter.recycler.ContactAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentContactsBinding
import de.deftk.openww.android.filter.ContactFilter
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.utils.ISearchProvider
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.ContactsViewModel
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.contacts.IContact

class ContactsFragment : ActionModeFragment<IContact, ContactAdapter.ContactViewHolder>(R.menu.contacts_actionmode_menu), ISearchProvider {

    private val contactsViewModel: ContactsViewModel by activityViewModels()
    private val args: ContactsFragmentArgs by navArgs()

    private lateinit var binding: FragmentContactsBinding
    private lateinit var searchView: SearchView

    private var scope: IOperatingScope? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentContactsBinding.inflate(inflater, container, false)

        contactsViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                contactsViewModel.resetBatchDeleteResponse()

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
            } else {
                setUIState(UIState.READY)
                actionMode?.finish()
            }
        }

        contactsViewModel.deleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                contactsViewModel.resetDeleteResponse() // mark as handled

            if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            } else if (response is Response.Success) {
                setUIState(UIState.READY)
            }
        }

        binding.contactsSwipeRefresh.setOnRefreshListener {
            loginViewModel.apiContext.value?.also { apiContext ->
                contactsViewModel.loadContacts(scope!!, apiContext)
                setUIState(UIState.LOADING)
            }
        }

        binding.fabAddContact.setOnClickListener {
            navController.navigate(ContactsFragmentDirections.actionContactsFragmentToEditContactFragment(scope!!.login, null))
        }

        loginViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                val foundScope = loginViewModel.apiContext.value?.findOperatingScope(args.scope)
                if (foundScope == null) {
                    Reporter.reportException(R.string.error_scope_not_found, args.scope, requireContext())
                    navController.popBackStack()
                    return@observe
                }
                if (!Feature.ADDRESSES.isAvailable(foundScope.effectiveRights)) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }
                if (scope != null) {
                    contactsViewModel.getFilteredContactsLiveData(scope!!).removeObservers(viewLifecycleOwner)
                    scope = foundScope
                    (adapter as ContactAdapter).scope = scope!!
                } else {
                    scope = foundScope
                }
                setTitle(foundScope.name)
                binding.contactList.adapter = adapter
                binding.contactList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

                contactsViewModel.getFilteredContactsLiveData(scope!!).observe(viewLifecycleOwner) { response ->
                    if (response is Response.Success) {
                        adapter.submitList(response.value)
                        binding.contactsEmpty.isVisible = response.value.isEmpty()
                        setUIState(UIState.READY)
                    } else if (response is Response.Failure) {
                        setUIState(UIState.ERROR)
                        Reporter.reportException(R.string.error_get_contacts_failed, response.exception, requireContext())
                    }
                    binding.contactsSwipeRefresh.isRefreshing = false
                }

                if (contactsViewModel.getAllContactsLiveData(scope!!).value == null) {
                    contactsViewModel.loadContacts(scope!!, apiContext)
                    setUIState(UIState.LOADING)
                }
                binding.fabAddContact.isVisible = scope!!.effectiveRights.contains(Permission.ADDRESSES_WRITE) || scope!!.effectiveRights.contains(Permission.ADDRESSES_ADMIN)
            } else {
                setUIState(UIState.DISABLED)
                binding.fabAddContact.isVisible = false
                if (scope != null)
                    adapter.submitList(emptyList())
            }
        }

        registerForContextMenu(binding.contactList)
        return binding.root
    }

    override fun createAdapter(): ActionModeAdapter<IContact, ContactAdapter.ContactViewHolder> {
        return ContactAdapter(scope!!, this)
    }

    override fun onItemClick(view: View, viewHolder: ContactAdapter.ContactViewHolder) {
        navController.navigate(ContactsFragmentDirections.actionContactsFragmentToReadContactFragment(viewHolder.binding.scope!!.login, viewHolder.binding.contact!!.id.toString()))
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val canModify = adapter.selectedItems.all { it.binding.scope!!.effectiveRights.contains(Permission.ADDRESSES_WRITE) || it.binding.scope!!.effectiveRights.contains(Permission.ADDRESSES_ADMIN) }
        menu.findItem(R.id.contacts_action_item_delete).isEnabled = canModify
        return super.onPrepareActionMode(mode, menu)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.contacts_action_item_delete -> {
                loginViewModel.apiContext.value?.also { apiContext ->
                    contactsViewModel.batchDelete(adapter.selectedItems.map { it.binding.scope!! to it.binding.contact!! }, apiContext)
                    setUIState(UIState.LOADING)
                }
            }
            else -> return false
        }
        return true
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_options_menu, menu)
        val searchItem = menu.findItem(R.id.list_options_item_search)
        searchView = searchItem.actionView as SearchView
        searchView.setQuery(contactsViewModel.filter.value?.smartSearchCriteria?.value, false) // restore recent search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filter = ContactFilter()
                filter.smartSearchCriteria.value = newText
                contactsViewModel.filter.value = filter
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
            if (scope!!.effectiveRights.contains(Permission.ADDRESSES_WRITE) || scope!!.effectiveRights.contains(Permission.ADDRESSES_ADMIN)) {
                requireActivity().menuInflater.inflate(R.menu.contacts_context_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.contactList.adapter as ContactAdapter
        return when (item.itemId) {
            R.id.contacts_context_item_edit -> {
                val contact = adapter.getItem(menuInfo.position)
                val action = ContactsFragmentDirections.actionContactsFragmentToEditContactFragment(scope!!.login, contact.id.toString())
                navController.navigate(action)
                true
            }
            R.id.contacts_context_item_delete -> {
                val contact = adapter.getItem(menuInfo.position)
                val apiContext = loginViewModel.apiContext.value ?: return false
                contactsViewModel.deleteContact(contact, scope!!, apiContext)
                setUIState(UIState.LOADING)
                true
            }
            else -> false
        }
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.contactsSwipeRefresh.isEnabled = newState.swipeRefreshEnabled
        binding.contactsSwipeRefresh.isRefreshing = newState.refreshing
        binding.contactList.isEnabled = newState.listEnabled
        binding.contactsEmpty.isVisible = newState.showEmptyIndicator
        binding.fabAddContact.isEnabled = newState == UIState.READY
    }
}