package de.deftk.openww.android.fragments.feature.contacts

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ContactDetailAdapter
import de.deftk.openww.android.adapter.recycler.ContactDetailClickListener
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentEditContactBinding
import de.deftk.openww.android.feature.contacts.ContactDetail
import de.deftk.openww.android.feature.contacts.ContactDetailType
import de.deftk.openww.android.feature.contacts.GenderTranslation
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.AndroidUtil
import de.deftk.openww.android.utils.ContactUtil
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.ContactsViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.implementation.feature.contacts.Contact
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.RemoteScope
import de.deftk.openww.api.model.feature.Modification
import de.deftk.openww.api.model.feature.contacts.IContact
import java.util.*

class EditContactFragment : AbstractFragment(true), ContactDetailClickListener {

    private val userViewModel: UserViewModel by activityViewModels()
    private val contactViewModel: ContactsViewModel by activityViewModels()
    private val args: EditContactFragmentArgs by navArgs()
    private val navController by lazy { findNavController() }
    private val adapter = ContactDetailAdapter(true, this)
    private val contact = MutableLiveData<IContact>()

    private lateinit var binding: FragmentEditContactBinding

    private var scope: IOperatingScope? = null
    private var editMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditContactBinding.inflate(inflater, container, false)

        binding.contactDetailList.adapter = adapter
        binding.contactDetailList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        userViewModel.apiContext.observe(viewLifecycleOwner) apiContext@ { apiContext ->
            if (apiContext != null) {
                val foundScope = apiContext.findOperatingScope(args.scope)
                if (foundScope == null) {
                    Reporter.reportException(R.string.error_scope_not_found, args.scope, requireContext())
                    navController.popBackStack()
                    return@apiContext
                }
                if (!foundScope.effectiveRights.contains(Permission.ADDRESSES_WRITE) && !foundScope.effectiveRights.contains(Permission.ADDRESSES_ADMIN)) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@apiContext
                }
                if (scope != null)
                    contactViewModel.getFilteredContactsLiveData(scope!!).removeObservers(viewLifecycleOwner)
                scope = foundScope
                contactViewModel.getFilteredContactsLiveData(scope!!).observe(viewLifecycleOwner) filtered@ { response ->
                    if (response is Response.Success) {
                        setUIState(UIState.READY)
                        if (args.contactId != null) {
                            // edit existing
                            editMode = true

                            val foundContact = contactViewModel.getFilteredContactsLiveData(scope!!).value?.valueOrNull()?.firstOrNull { it.id.toString() == args.contactId }
                            if (foundContact == null) {
                                Reporter.reportException(R.string.error_contact_not_found, args.contactId!!, requireContext())
                                navController.popBackStack()
                                return@filtered
                            }
                            contact.value = foundContact!!
                        } else {
                            // add new
                            editMode = false
                            val modification = Modification(RemoteScope("", "", -1, null, false), Date())
                            contact.value = Contact(-1, _modified = modification, created = modification)
                        }
                    } else if (response is Response.Failure) {
                        setUIState(UIState.ERROR)
                        Reporter.reportException(R.string.error_get_members_failed, response.exception, requireContext())
                    }
                }

                if (contactViewModel.getAllContactsLiveData(scope!!).value == null) {
                    contactViewModel.loadContacts(scope!!, apiContext)
                    setUIState(UIState.LOADING)
                }
            } else {
                adapter.submitList(emptyList())
                binding.fabAddContactDetail.isVisible = false
                setUIState(UIState.DISABLED)
            }
        }

        contact.observe(viewLifecycleOwner) { contact ->
            val details = ContactUtil.extractContactDetails(contact)
            adapter.submitList(details)
            binding.contactDetailsEmpty.isVisible = details.isEmpty()
        }

        binding.fabAddContactDetail.setOnClickListener {
            val usedDetails = (binding.contactDetailList.adapter as ContactDetailAdapter).currentList.map { it.type }
            val availableDetails = ContactDetailType.values().filter { !usedDetails.contains(it) }

            val builder = getDefaultDialogBuilder()
            builder.setTitle(R.string.add_contact_detail)
            val layout = RelativeLayout(requireContext())

            val lpTypeSpinner = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            lpTypeSpinner.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
            lpTypeSpinner.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
            lpTypeSpinner.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
            val typeSpinner = Spinner(requireContext())
            val typeSpinnerId = View.generateViewId()
            typeSpinner.id = typeSpinnerId
            typeSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, availableDetails.map { getString(it.description) })
            typeSpinner.setSelection(0)
            layout.addView(typeSpinner, lpTypeSpinner)

            val lpGenderSpinner = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            lpGenderSpinner.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
            lpGenderSpinner.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
            lpGenderSpinner.addRule(RelativeLayout.BELOW, typeSpinnerId)
            val genderSpinner = Spinner(requireContext())
            val genderSpinnerId = View.generateViewId()
            genderSpinner.id = genderSpinnerId
            genderSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, GenderTranslation.values().map { getString(it.translation) })
            genderSpinner.setSelection(0)
            layout.addView(genderSpinner, lpGenderSpinner)

            val lpEditText = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            lpEditText.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
            lpEditText.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
            lpEditText.addRule(RelativeLayout.BELOW, typeSpinnerId)
            val editText = EditText(requireContext())
            val editTextId = View.generateViewId()
            editText.id = editTextId
            editText.hint = getString(R.string.value)
            layout.addView(editText, lpEditText)

            typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (availableDetails[position] == ContactDetailType.GENDER) {
                        editText.isVisible = false
                        genderSpinner.isVisible = true
                    } else {
                        editText.isVisible = true
                        genderSpinner.isVisible = false
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }


            builder.setView(layout)

            builder.setPositiveButton(R.string.confirm) { _, _ ->
                val detail = availableDetails[typeSpinner.selectedItemPosition]
                val value: Any = if (genderSpinner.isVisible) {
                    // gender
                    GenderTranslation.values()[genderSpinner.selectedItemPosition].gender
                } else {
                    // string
                    editText.text.toString()
                }
                contact.value = ContactUtil.editContactDetail(detail, value, contact.value!!)
            }

            builder.show()
        }

        contactViewModel.editResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                contactViewModel.resetEditResponse()

            if (response is Response.Success) {
                setUIState(UIState.READY)
                AndroidUtil.hideKeyboard(requireActivity(), requireView())
                navController.popBackStack()
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_save_changes_failed, response.exception, requireContext())
            }
        }

        return binding.root
    }

    override fun onContactDetailEdit(detail: ContactDetail) {
        if (detail.type == ContactDetailType.GENDER) {
            // gender dialog
            val builder = getDefaultDialogBuilder()
            builder.setTitle(R.string.edit_contact_detail)
            val spinner = Spinner(requireContext())
            spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, GenderTranslation.values().map { getString(it.translation) })
            spinner.setSelection(GenderTranslation.values().indexOfFirst { it.gender == detail.value })
            builder.setView(spinner)

            builder.setPositiveButton(R.string.confirm) { _, _ ->
                val gender = GenderTranslation.values()[spinner.selectedItemPosition].gender
                contact.value = ContactUtil.editContactDetail(detail.type, gender, contact.value!!)
            }
            builder.show()
        } else {
            // string dialog
            val builder = getDefaultDialogBuilder()
            builder.setTitle(R.string.edit_contact_detail)
            val editText = EditText(requireContext())
            editText.hint = getString(R.string.value)
            editText.setText(detail.value.toString())
            builder.setView(editText)

            builder.setPositiveButton(R.string.confirm) { _, _ ->
                val text = editText.text.toString()
                if (text.isNotBlank()) {
                    contact.value = ContactUtil.editContactDetail(detail.type, text, contact.value!!)
                }
            }
            builder.show()
        }
    }

    override fun onContactDetailRemove(detail: ContactDetail) {
        contact.value = ContactUtil.removeContactDetail(detail.type, contact.value!!)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.edit_options_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.edit_options_item_save) {
            val apiContext = userViewModel.apiContext.value ?: return false

            if (editMode) {
                contactViewModel.editContact(contact.value!!, scope!!, apiContext)
                setUIState(UIState.LOADING)
            } else {
                contactViewModel.addContact(contact.value!!, scope!!, apiContext)
                setUIState(UIState.LOADING)
            }

            return true
        }
        return false
    }

    private fun getDefaultDialogBuilder(): AlertDialog.Builder {
        val builder = AlertDialog.Builder(requireContext())
        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }
        return builder
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.contactDetailList.isEnabled = newState.listEnabled
        binding.contactDetailsEmpty.isVisible = newState.showEmptyIndicator
        binding.fabAddContactDetail.isEnabled = newState == UIState.READY
    }
}