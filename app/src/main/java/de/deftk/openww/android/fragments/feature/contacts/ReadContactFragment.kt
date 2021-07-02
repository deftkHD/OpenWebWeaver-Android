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
import de.deftk.openww.android.adapter.recycler.ContactDetailAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentReadContactBinding
import de.deftk.openww.android.utils.ContactUtil
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.ContactsViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.contacts.IContact

class ReadContactFragment : Fragment() {

    private val args: ReadContactFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val contactsViewModel: ContactsViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentReadContactBinding
    private lateinit var contact: IContact
    private lateinit var scope: IOperatingScope

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadContactBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        scope = userViewModel.apiContext.value?.findOperatingScope(args.scope) ?: error("Failed to find operating scope ${args.scope}")

        val adapter = ContactDetailAdapter(false, null)
        binding.contactDetailList.adapter = adapter
        binding.contactDetailList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        contactsViewModel.getContactsLiveData(scope).observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                val queried = response.value.firstOrNull { it.id.toString() == args.contactId }
                if (queried != null) {
                    contact = queried
                } else {
                    navController.popBackStack()
                }
                adapter.submitList(ContactUtil.extractContactDetails(contact))
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_contacts_failed, response.exception, requireContext())
            }
        }

        contactsViewModel.deleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                contactsViewModel.resetDeleteResponse()
            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }

        binding.fabEditContact.setOnClickListener {
            navController.navigate(ReadContactFragmentDirections.actionReadContactFragmentToEditContactFragment(scope.login, contact.id.toString(), getString(R.string.edit_contact)))
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                binding.fabEditContact.isVisible = scope.effectiveRights.contains(Permission.ADDRESSES_ADMIN)
            } else {
                navController.popBackStack(R.id.contactsGroupFragment, false)
            }
        }

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (scope.effectiveRights.contains(Permission.BOARD_ADMIN))
            inflater.inflate(R.menu.simple_edit_item_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                val action = ReadContactFragmentDirections.actionReadContactFragmentToEditContactFragment(scope.login, contact.id.toString(), getString(R.string.edit_contact))
                navController.navigate(action)
                true
            }
            R.id.menu_item_delete -> {
                val apiContext = userViewModel.apiContext.value ?: return false
                contactsViewModel.deleteContact(contact, scope, apiContext)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}