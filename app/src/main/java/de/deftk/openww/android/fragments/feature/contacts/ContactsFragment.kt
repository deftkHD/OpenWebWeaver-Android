package de.deftk.openww.android.fragments.feature.contacts

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.activities.MainActivity
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
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.contacts.IContact

class ContactsFragment : ActionModeFragment<IContact, ContactAdapter.ContactViewHolder>(R.menu.contacts_actionmode_menu), ISearchProvider {

    private val userViewModel: UserViewModel by activityViewModels()
    private val contactsViewModel: ContactsViewModel by activityViewModels()
    private val args: ContactsFragmentArgs by navArgs()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentContactsBinding
    private lateinit var searchView: SearchView

    private var scope: IOperatingScope? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentContactsBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        (requireActivity() as? MainActivity?)?.searchProvider = this

        contactsViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                contactsViewModel.resetBatchDeleteResponse()

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
                binding.progressContacts.isVisible = false
            } else {
                actionMode?.finish()
            }
        }

        binding.contactsSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                contactsViewModel.loadContacts(scope!!, apiContext)
            }
        }

        binding.fabAddContact.setOnClickListener {
            navController.navigate(ContactsFragmentDirections.actionContactsFragmentToEditContactFragment(scope!!.login, null, getString(R.string.add_contact)))
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                val foundScope = userViewModel.apiContext.value?.findOperatingScope(args.login)
                if (foundScope == null) {
                    Reporter.reportException(R.string.error_scope_not_found, args.login, requireContext())
                    navController.popBackStack()
                    return@observe
                }
                if (!Feature.ADDRESSES.isAvailable(foundScope.effectiveRights)) {
                    Reporter.reportException(R.string.feature_not_available, args.login, requireContext())
                    navController.popBackStack()
                    return@observe
                }
                if (scope != null) {
                    contactsViewModel.getFilteredContactsLiveData(scope!!).removeObservers(viewLifecycleOwner)
                    scope = foundScope
                    (adapter as ContactAdapter).scope = scope!!
                } else {
                    scope = foundScope
                    binding.contactList.adapter = adapter
                    binding.contactList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
                }

                contactsViewModel.getFilteredContactsLiveData(scope!!).observe(viewLifecycleOwner) { response ->
                    if (response is Response.Success) {
                        adapter.submitList(response.value)
                        binding.contactsEmpty.isVisible = response.value.isEmpty()
                    } else if (response is Response.Failure) {
                        Reporter.reportException(R.string.error_get_contacts_failed, response.exception, requireContext())
                    }
                    binding.progressContacts.isVisible = false
                    binding.contactsSwipeRefresh.isRefreshing = false
                }

                contactsViewModel.loadContacts(scope!!, apiContext)
                binding.fabAddContact.isVisible = scope!!.effectiveRights.contains(Permission.ADDRESSES_WRITE) || scope!!.effectiveRights.contains(Permission.ADDRESSES_ADMIN)
            } else {
                binding.contactsEmpty.isVisible = false
                binding.progressContacts.isVisible = true
                binding.fabAddContact.isVisible = false
                adapter.submitList(emptyList())
            }
        }

        setHasOptionsMenu(true)
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
        menu.findItem(R.id.contacts_action_delete).isEnabled = canModify
        return super.onPrepareActionMode(mode, menu)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.contacts_action_delete -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    contactsViewModel.batchDelete(adapter.selectedItems.map { it.binding.scope!! to it.binding.contact!! }, apiContext)
                    binding.progressContacts.isVisible = true
                }
            }
            else -> return false
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
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

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            if (scope!!.effectiveRights.contains(Permission.ADDRESSES_WRITE) || scope!!.effectiveRights.contains(Permission.ADDRESSES_ADMIN)) {
                requireActivity().menuInflater.inflate(R.menu.simple_edit_item_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.contactList.adapter as ContactAdapter
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                val contact = adapter.getItem(menuInfo.position)
                val action = ContactsFragmentDirections.actionContactsFragmentToEditContactFragment(scope!!.login, contact.id.toString(), getString(R.string.edit_contact))
                navController.navigate(action)
                true
            }
            R.id.menu_item_delete -> {
                val contact = adapter.getItem(menuInfo.position)
                val apiContext = userViewModel.apiContext.value ?: return false
                contactsViewModel.deleteContact(contact, scope!!, apiContext)
                true
            }
            else -> false
        }
    }

    override fun onDestroy() {
        (requireActivity() as? MainActivity?)?.searchProvider = null
        super.onDestroy()
    }

}