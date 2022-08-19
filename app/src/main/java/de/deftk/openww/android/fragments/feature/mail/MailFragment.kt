package de.deftk.openww.android.fragments.feature.mail

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.AdapterView
import android.widget.EditText
import android.widget.SearchView
import android.widget.Spinner
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.MailFolderAdapter
import de.deftk.openww.android.adapter.recycler.ActionModeAdapter
import de.deftk.openww.android.adapter.recycler.MailAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentMailBinding
import de.deftk.openww.android.filter.MailFilter
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.utils.ISearchProvider
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.MailboxViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.mailbox.IEmail
import de.deftk.openww.api.model.feature.mailbox.IEmailFolder

class MailFragment: ActionModeFragment<Pair<IEmail, IEmailFolder>, MailAdapter.MailViewHolder>(R.menu.mail_actionmode_menu), ISearchProvider {

    private val userViewModel: UserViewModel by activityViewModels()
    private val mailboxViewModel: MailboxViewModel by activityViewModels()
    private val navController: NavController by lazy { findNavController() }

    private lateinit var binding: FragmentMailBinding
    private lateinit var toolbarSpinner: Spinner
    private lateinit var searchView: SearchView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMailBinding.inflate(inflater, container, false)

        toolbarSpinner = requireActivity().findViewById(R.id.toolbar_spinner)
        toolbarSpinner.isVisible = true
        toolbarSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val folder = toolbarSpinner.getItemAtPosition(position) as IEmailFolder
                if (folder.id != mailboxViewModel.currentFolder.value?.id) {
                    userViewModel.apiContext.value?.also { apiContext ->
                        enableUI(false)
                        mailboxViewModel.selectFolder(folder, apiContext)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.mailList.adapter = adapter
        binding.mailList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        mailboxViewModel.currentFilteredMails.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value.map { it to mailboxViewModel.currentFolder.value!! })
                binding.mailEmpty.isVisible = response.value.isEmpty()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_emails_failed, response.exception, requireContext())
            }
            enableUI(true)
            binding.mailSwipeRefresh.isRefreshing = false
        }

        binding.mailSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                mailboxViewModel.cleanCache()
                mailboxViewModel.loadFolders(apiContext)
            }
        }

        mailboxViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                mailboxViewModel.resetBatchDeleteResponse()
            enableUI(true)

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
            } else {
                actionMode?.finish()
            }
        }

        mailboxViewModel.batchMoveResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                mailboxViewModel.resetBatchMoveResponse()
            enableUI(true)

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                Reporter.reportException(R.string.error_move_failed, failure.first().exception, requireContext())
            } else {
                actionMode?.finish()
            }
        }

        mailboxViewModel.batchEmailSetResponse.observe(viewLifecycleOwner) { responses ->
            if (responses != null)
                mailboxViewModel.resetEmailSetResponse()
            enableUI(true)

            responses?.forEach { response ->
                if (response is Response.Success) {
                    val index = adapter.currentList.indexOfFirst { it.first.id == response.value.id }
                    adapter.notifyItemChanged(index)
                } else if (response is Response.Failure) {
                    Reporter.reportException(R.string.error_set_email_failed, response.exception, requireContext())
                    return@forEach
                }
            }
            actionMode?.finish()
        }

        mailboxViewModel.foldersResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                if ((toolbarSpinner.adapter as? MailFolderAdapter?)?.elements != response.value) {
                    toolbarSpinner.adapter = MailFolderAdapter(requireContext(), response.value)
                } else {
                    binding.mailSwipeRefresh.isRefreshing = false
                }
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_folders_failed, response.exception, requireContext())
                toolbarSpinner.adapter = null
                binding.mailSwipeRefresh.isRefreshing = false
            }
            enableUI(true)
        }

        mailboxViewModel.currentFolder.observe(viewLifecycleOwner) { folder ->
            val adapter = (toolbarSpinner.adapter as? MailFolderAdapter?)
            val index = adapter?.elements?.indexOf(folder) ?: -1
            if (index != -1) {
                toolbarSpinner.setSelection(index)
            }
            enableUI(true)
        }

        binding.fabMailAdd.setOnClickListener {
            navController.navigate(MailFragmentDirections.actionMailFragmentToWriteMailFragment())
            toolbarSpinner.isVisible = false
            toolbarSpinner.adapter = null
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (!Feature.MAILBOX.isAvailable(apiContext.user.effectiveRights)) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }
                (adapter as MailAdapter).user = apiContext.user
                mailboxViewModel.cleanCache()
                mailboxViewModel.loadFolders(apiContext)
                if (mailboxViewModel.foldersResponse.value == null)
                    enableUI(false)

                //TODO not sure about this permissions
                binding.fabMailAdd.isVisible = apiContext.user.effectiveRights.contains(Permission.MAILBOX_WRITE) || apiContext.user.effectiveRights.contains(Permission.MAILBOX_ADMIN)
            } else {
                binding.fabMailAdd.isVisible = false
                binding.mailEmpty.isVisible = false
                toolbarSpinner.adapter = null
                adapter.submitList(emptyList())
                enableUI(false)
            }
        }

        mailboxViewModel.folderPostResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                mailboxViewModel.resetPostResponse() // mark as handled
            enableUI(true)

            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_save_changes_failed, response.exception, requireContext())
            }
        }

        registerForContextMenu(binding.mailList)
        return binding.root
    }

    override fun createAdapter(): ActionModeAdapter<Pair<IEmail, IEmailFolder>, MailAdapter.MailViewHolder> {
        return MailAdapter(this, userViewModel.apiContext.value!!.user)
    }

    override fun onItemClick(view: View, viewHolder: MailAdapter.MailViewHolder) {
        navController.navigate(MailFragmentDirections.actionMailFragmentToReadMailFragment(viewHolder.binding.folder!!.id, viewHolder.binding.email!!.id))
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val user = userViewModel.apiContext.value?.user
        val canModify = user?.effectiveRights?.contains(Permission.MAILBOX_WRITE) == true || user?.effectiveRights?.contains(Permission.MAILBOX_ADMIN) == true
        menu.findItem(R.id.mail_action_item_move).isEnabled = canModify
        menu.findItem(R.id.mail_action_item_delete).isEnabled = canModify
        return super.onPrepareActionMode(mode, menu)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mail_action_item_move -> {
                val folders = mailboxViewModel.foldersResponse.value?.valueOrNull()?.filter { it.id != mailboxViewModel.currentFolder.value?.id } ?: emptyList()
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.move_to)
                    .setIcon(R.drawable.ic_move_to_inbox_24)
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setAdapter(MailFolderAdapter(requireContext(), folders)) { _, which ->
                        userViewModel.apiContext.value?.also { apiContext ->
                            mailboxViewModel.batchMove(adapter.selectedItems.map { it.binding.email!! }, mailboxViewModel.currentFolder.value!!, folders[which], apiContext)
                            enableUI(false)
                        }
                    }
                    .create()
                    .show()
            }
            R.id.mail_action_item_delete -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    mailboxViewModel.batchDelete(adapter.selectedItems.map { it.binding.email!! }, mailboxViewModel.currentFolder.value!!, apiContext)
                    enableUI(false)
                }
            }
            R.id.mail_action_item_set_read -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    mailboxViewModel.batchSetEmails(adapter.selectedItems.map { it.binding.email!! }, mailboxViewModel.currentFolder.value!!, null, false, apiContext)
                    enableUI(false)
                }
            }
            R.id.mail_action_item_set_unread -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    mailboxViewModel.batchSetEmails(adapter.selectedItems.map { it.binding.email!! }, mailboxViewModel.currentFolder.value!!, null, true, apiContext)
                    enableUI(false)
                }
            }
            else -> return false
        }
        return true
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_options_menu, menu)
        menuInflater.inflate(R.menu.mail_options_menu, menu)
        val searchItem = menu.findItem(R.id.list_options_item_search)
        searchView = searchItem.actionView as SearchView
        searchView.setQuery(mailboxViewModel.mailFilter.value?.smartSearchCriteria?.value, false) // restore recent search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filter = MailFilter()
                filter.smartSearchCriteria.value = newText
                mailboxViewModel.mailFilter.value = filter
                return true
            }
        })
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.mail_options_item_add_folder) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(R.string.create_new_folder)

            val input = EditText(requireContext())
            input.hint = getString(R.string.name)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            builder.setPositiveButton(R.string.confirm) { _, _ ->
                userViewModel.apiContext.value?.apply {
                    mailboxViewModel.addFolder(input.text.toString(), this)
                    enableUI(false)
                }
            }
            builder.setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
            return true
        } else return false
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
            val (email, _) = (binding.mailList.adapter as MailAdapter).getItem(menuInfo.position)
            userViewModel.apiContext.value?.also { apiContext ->
                if (apiContext.user.effectiveRights.contains(Permission.MAILBOX_WRITE) || apiContext.user.effectiveRights.contains(Permission.MAILBOX_ADMIN)) {
                    requireActivity().menuInflater.inflate(R.menu.mail_context_menu, menu)
                    menu.findItem(R.id.mail_context_item_set_unread).isVisible = !email.unread
                    menu.findItem(R.id.mail_context_item_set_read).isVisible = email.unread
                }
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.mailList.adapter as MailAdapter
        when (item.itemId) {
            R.id.mail_context_item_move -> {
                val mailItem = adapter.getItem(menuInfo.position)
                val folders = mailboxViewModel.foldersResponse.value?.valueOrNull()?.filter { it.id != mailItem.second.id } ?: emptyList()
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.move_to)
                    .setIcon(R.drawable.ic_move_to_inbox_24)
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setAdapter(MailFolderAdapter(requireContext(), folders)) { _, which ->
                        userViewModel.apiContext.value?.also { apiContext ->
                            mailboxViewModel.moveEmail(mailItem.first, mailItem.second, folders[which], apiContext)
                            enableUI(false)
                        }
                    }
                    .create()
                    .show()
            }
            R.id.mail_context_item_delete -> {
                val mailItem = adapter.getItem(menuInfo.position)
                userViewModel.apiContext.value?.also { apiContext ->
                    mailboxViewModel.deleteEmail(mailItem.first, mailItem.second, true, apiContext)
                    enableUI(false)
                }
            }
            R.id.mail_context_item_set_read -> {
                val mailItem = adapter.getItem(menuInfo.position)
                userViewModel.apiContext.value?.also { apiContext ->
                    mailboxViewModel.batchSetEmails(listOf(mailItem.first), mailItem.second, null, false, apiContext)
                    enableUI(false)
                }
            }
            R.id.mail_context_item_set_unread -> {
                val mailItem = adapter.getItem(menuInfo.position)
                userViewModel.apiContext.value?.also { apiContext ->
                    mailboxViewModel.batchSetEmails(listOf(mailItem.first), mailItem.second, null, true, apiContext)
                    enableUI(false)
                }
            }
            else -> return false
        }
        return true
    }

    override fun onResume() {
        toolbarSpinner.isVisible = true
        super.onResume()
    }

    override fun onStop() {
        toolbarSpinner.isVisible = false
        super.onStop()
    }

    override fun onUIStateChanged(enabled: Boolean) {
        binding.mailSwipeRefresh.isEnabled = enabled
        binding.mailList.isEnabled = enabled
        binding.fabMailAdd.isEnabled = enabled
    }
}