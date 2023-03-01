package de.deftk.openww.android.fragments.feature.contacts

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ContactDetailAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentReadContactBinding
import de.deftk.openww.android.fragments.ContextualFragment
import de.deftk.openww.android.utils.ContactUtil
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.ContactsViewModel
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.contacts.IContact

class ReadContactFragment : ContextualFragment(true) {

    private val args: ReadContactFragmentArgs by navArgs()
    private val contactsViewModel: ContactsViewModel by activityViewModels()
    private val adapter = ContactDetailAdapter(true, null)

    private lateinit var binding: FragmentReadContactBinding
    private lateinit var contact: IContact

    private var scope: IOperatingScope? = null
    private var deleted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadContactBinding.inflate(inflater, container, false)

        binding.contactDetailList.adapter = adapter
        binding.contactDetailList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        contactsViewModel.deleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                contactsViewModel.resetDeleteResponse()

            if (response is Response.Success) {
                setUIState(UIState.READY)
                deleted = true
                navController.popBackStack()
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }

        binding.fabEditContact.setOnClickListener {
            navController.navigate(ReadContactFragmentDirections.actionReadContactFragmentToEditContactFragment(scope!!.login, contact.id.toString(), getString(R.string.edit_contact)))
        }

        loginViewModel.apiContext.observe(viewLifecycleOwner) apiContext@ { apiContext ->
            if (apiContext != null) {
                val foundScope = loginViewModel.apiContext.value?.findOperatingScope(args.scope)
                if (foundScope == null) {
                    Reporter.reportException(R.string.error_scope_not_found, args.scope, requireContext())
                    navController.popBackStack()
                    return@apiContext
                }
                if (!Feature.ADDRESSES.isAvailable(foundScope.effectiveRights)) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@apiContext
                }
                if (scope != null)
                    contactsViewModel.getFilteredContactsLiveData(scope!!).removeObservers(viewLifecycleOwner)
                scope = foundScope
                contactsViewModel.getFilteredContactsLiveData(scope!!).observe(viewLifecycleOwner) filtered@ { response ->
                    if (deleted)
                        return@filtered

                    if (response is Response.Success) {
                        setUIState(UIState.READY)
                        val queried = response.value.firstOrNull { it.id.toString() == args.contactId }
                        if (queried != null) {
                            contact = queried
                        } else {
                            Reporter.reportException(R.string.error_contact_not_found, args.contactId, requireContext())
                            navController.popBackStack()
                            return@filtered
                        }
                        adapter.submitList(ContactUtil.extractContactDetails(contact))
                    } else if (response is Response.Failure) {
                        setUIState(UIState.ERROR)
                        Reporter.reportException(R.string.error_get_contacts_failed, response.exception, requireContext())
                        navController.popBackStack()
                        return@filtered
                    }
                }
                if (contactsViewModel.getAllContactsLiveData(scope!!).value == null) {
                    contactsViewModel.loadContacts(scope!!, apiContext)
                    setUIState(UIState.LOADING)
                }
                binding.fabEditContact.isVisible = scope!!.effectiveRights.contains(Permission.ADDRESSES_WRITE) || scope!!.effectiveRights.contains(Permission.ADDRESSES_ADMIN)
            } else {
                adapter.submitList(emptyList())
                binding.fabEditContact.isVisible = false
                setUIState(UIState.DISABLED)
            }
        }

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (scope!!.effectiveRights.contains(Permission.ADDRESSES_WRITE) || scope!!.effectiveRights.contains(Permission.ADDRESSES_ADMIN))
            menuInflater.inflate(R.menu.contacts_context_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.contacts_context_item_edit -> {
                val action = ReadContactFragmentDirections.actionReadContactFragmentToEditContactFragment(scope!!.login, contact.id.toString(), getString(R.string.edit_contact))
                navController.navigate(action)
                true
            }
            R.id.contacts_context_item_delete -> {
                val apiContext = loginViewModel.apiContext.value ?: return false
                contactsViewModel.deleteContact(contact, scope!!, apiContext)
                setUIState(UIState.LOADING)
                true
            }
            else -> false
        }
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.contactDetailList.isEnabled = newState.listEnabled
        binding.fabEditContact.isEnabled = newState == UIState.READY
    }
}