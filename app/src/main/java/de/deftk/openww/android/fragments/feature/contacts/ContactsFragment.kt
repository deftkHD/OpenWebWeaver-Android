package de.deftk.openww.android.fragments.feature.contacts

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ContactAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentContactsBinding
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.ContactsViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.Permission

class ContactsFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val contactsViewModel: ContactsViewModel by activityViewModels()
    private val args: ContactsFragmentArgs by navArgs()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentContactsBinding
    private lateinit var scope: IOperatingScope

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentContactsBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        scope = userViewModel.apiContext.value?.findOperatingScope(args.login) ?: error("Failed to find operating scope ${args.login}") //TODO update scope with apiContext observer & pop if scope == null

        val adapter = ContactAdapter(scope)
        binding.contactList.adapter = adapter
        binding.contactList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        contactsViewModel.getContactsLiveData(scope).observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value)
                binding.contactsEmpty.isVisible = response.value.isEmpty()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_contacts_failed, response.exception, requireContext())
            }
            binding.progressContacts.isVisible = false
            binding.contactsSwipeRefresh.isRefreshing = false
        }

        binding.contactsSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                contactsViewModel.loadContacts(scope, apiContext)
            }
        }

        binding.fabAddContact.setOnClickListener {
            navController.navigate(ContactsFragmentDirections.actionContactsFragmentToEditContactFragment(scope.login, null, getString(R.string.add_contact)))
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                contactsViewModel.loadContacts(scope, apiContext)
            } else {
                navController.popBackStack(R.id.contactsGroupFragment, false)
            }
        }

        registerForContextMenu(binding.contactList)
        return binding.root
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            if (scope.effectiveRights.contains(Permission.ADDRESSES_ADMIN)) {
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
                val action = ContactsFragmentDirections.actionContactsFragmentToEditContactFragment(scope.login, contact.id.toString(), getString(R.string.edit_contact))
                navController.navigate(action)
                true
            }
            R.id.menu_item_delete -> {
                val contact = adapter.getItem(menuInfo.position)
                val apiContext = userViewModel.apiContext.value ?: return false
                contactsViewModel.deleteContact(contact, scope, apiContext)
                true
            }
            else -> false
        }
    }

}