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
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.mailbox.IEmail
import de.deftk.openww.api.model.feature.mailbox.IEmailFolder

class MailFragment: ActionModeFragment<Pair<IEmail, IEmailFolder>, MailAdapter.MailViewHolder>(R.menu.mail_actionmode_menu), ISearchProvider {

    private val mailboxViewModel: MailboxViewModel by activityViewModels()

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
                    loginViewModel.apiContext.value?.also { apiContext ->
                        mailboxViewModel.selectFolder(folder, apiContext)
                        setUIState(UIState.LOADING)
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
                setUIState(if (response.value.isEmpty()) UIState.EMPTY else UIState.READY)
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_get_emails_failed, response.exception, requireContext())
            }
        }

        binding.mailSwipeRefresh.setOnRefreshListener {
            loginViewModel.apiContext.value?.also { apiContext ->
                mailboxViewModel.resetScopedData()
                mailboxViewModel.loadFolders(apiContext)
                setUIState(UIState.LOADING)
            }
        }

        mailboxViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                mailboxViewModel.resetBatchDeleteResponse()

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
            } else {
                setUIState(UIState.READY)
                actionMode?.finish()
            }
        }

        mailboxViewModel.batchMoveResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                mailboxViewModel.resetBatchMoveResponse()

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_move_failed, failure.first().exception, requireContext())
            } else {
                setUIState(UIState.READY)
                actionMode?.finish()
            }
        }

        mailboxViewModel.batchEmailSetResponse.observe(viewLifecycleOwner) { responses ->
            if (responses != null)
                mailboxViewModel.resetEmailSetResponse()

            responses?.forEach { response ->
                if (response is Response.Success) {
                    val index = adapter.currentList.indexOfFirst { it.first.id == response.value.id }
                    adapter.notifyItemChanged(index)
                } else if (response is Response.Failure) {
                    setUIState(UIState.ERROR)
                    Reporter.reportException(R.string.error_set_email_failed, response.exception, requireContext())
                    return@forEach
                }
            }
            setUIState(UIState.READY)
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
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_get_folders_failed, response.exception, requireContext())
                toolbarSpinner.adapter = null
                binding.mailSwipeRefresh.isRefreshing = false
            }
        }

        mailboxViewModel.currentFolder.observe(viewLifecycleOwner) { folder ->
            val adapter = (toolbarSpinner.adapter as? MailFolderAdapter?)
            val index = adapter?.elements?.indexOf(folder) ?: -1
            if (index != -1) {
                toolbarSpinner.setSelection(index)
            }
        }

        binding.fabMailAdd.setOnClickListener {
            navController.navigate(MailFragmentDirections.actionGlobalWriteMailFragment())
            toolbarSpinner.isVisible = false
            toolbarSpinner.adapter = null
        }

        loginViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (!Feature.MAILBOX.isAvailable(apiContext.user.effectiveRights)) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }
                (adapter as MailAdapter).user = apiContext.user
                if (mailboxViewModel.foldersResponse.value == null) {
                    mailboxViewModel.cleanCache()
                    mailboxViewModel.loadFolders(apiContext)
                    setUIState(UIState.LOADING)
                }

                //TODO not sure about this permissions
                binding.fabMailAdd.isVisible = apiContext.user.effectiveRights.contains(Permission.MAILBOX_WRITE) || apiContext.user.effectiveRights.contains(Permission.MAILBOX_ADMIN)
            } else {
                binding.fabMailAdd.isVisible = false
                toolbarSpinner.adapter = null
                adapter.submitList(emptyList())
                setUIState(UIState.DISABLED)
            }
        }

        mailboxViewModel.folderPostResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                mailboxViewModel.resetPostResponse() // mark as handled

            if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_save_changes_failed, response.exception, requireContext())
            } else if (response is Response.Success) {
                setUIState(UIState.READY)
            }
        }

        registerForContextMenu(binding.mailList)
        return binding.root
    }

    override fun createAdapter(): ActionModeAdapter<Pair<IEmail, IEmailFolder>, MailAdapter.MailViewHolder> {
        return MailAdapter(this, loginViewModel.apiContext.value!!.user)
    }

    override fun onItemClick(view: View, viewHolder: MailAdapter.MailViewHolder) {
        navController.navigate(MailFragmentDirections.actionMailFragmentToReadMailFragment(viewHolder.binding.folder!!.id, viewHolder.binding.email!!.id))
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val user = loginViewModel.apiContext.value?.user
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
                        loginViewModel.apiContext.value?.also { apiContext ->
                            mailboxViewModel.batchMove(adapter.selectedItems.map { it.binding.email!! }, mailboxViewModel.currentFolder.value!!, folders[which], apiContext)
                            setUIState(UIState.LOADING)
                        }
                    }
                    .create()
                    .show()
            }
            R.id.mail_action_item_delete -> {
                loginViewModel.apiContext.value?.also { apiContext ->
                    mailboxViewModel.batchDelete(adapter.selectedItems.map { it.binding.email!! }, mailboxViewModel.currentFolder.value!!, apiContext)
                    setUIState(UIState.LOADING)
                }
            }
            R.id.mail_action_item_set_read -> {
                loginViewModel.apiContext.value?.also { apiContext ->
                    mailboxViewModel.batchSetEmails(adapter.selectedItems.map { it.binding.email!! }, mailboxViewModel.currentFolder.value!!, null, false, apiContext)
                    setUIState(UIState.LOADING)
                }
            }
            R.id.mail_action_item_set_unread -> {
                loginViewModel.apiContext.value?.also { apiContext ->
                    mailboxViewModel.batchSetEmails(adapter.selectedItems.map { it.binding.email!! }, mailboxViewModel.currentFolder.value!!, null, true, apiContext)
                    setUIState(UIState.LOADING)
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
                loginViewModel.apiContext.value?.apply {
                    mailboxViewModel.addFolder(input.text.toString(), this)
                    setUIState(UIState.LOADING)
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
            loginViewModel.apiContext.value?.also { apiContext ->
                if (apiContext.user.effectiveRights.contains(Permission.MAILBOX_WRITE) || apiContext.user.effectiveRights.contains(Permission.MAILBOX_ADMIN)) {
                    requireActivity().menuInflater.inflate(R.menu.mail_context_menu, menu)
                    menu.findItem(R.id.mail_context_item_set_unread).isVisible = email.unread == false
                    menu.findItem(R.id.mail_context_item_set_read).isVisible = email.unread == true
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
                        loginViewModel.apiContext.value?.also { apiContext ->
                            mailboxViewModel.moveEmail(mailItem.first, mailItem.second, folders[which], apiContext)
                            setUIState(UIState.LOADING)
                        }
                    }
                    .create()
                    .show()
            }
            R.id.mail_context_item_delete -> {
                val mailItem = adapter.getItem(menuInfo.position)
                loginViewModel.apiContext.value?.also { apiContext ->
                    mailboxViewModel.deleteEmail(mailItem.first, mailItem.second, true, apiContext)
                    setUIState(UIState.LOADING)
                }
            }
            R.id.mail_context_item_set_read -> {
                val mailItem = adapter.getItem(menuInfo.position)
                loginViewModel.apiContext.value?.also { apiContext ->
                    mailboxViewModel.batchSetEmails(listOf(mailItem.first), mailItem.second, null, false, apiContext)
                    setUIState(UIState.LOADING)
                }
            }
            R.id.mail_context_item_set_unread -> {
                val mailItem = adapter.getItem(menuInfo.position)
                loginViewModel.apiContext.value?.also { apiContext ->
                    mailboxViewModel.batchSetEmails(listOf(mailItem.first), mailItem.second, null, true, apiContext)
                    setUIState(UIState.LOADING)
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

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.fabMailAdd.isEnabled = newState == UIState.READY
        binding.mailList.isEnabled = newState.listEnabled
        binding.mailEmpty.isVisible = newState.showEmptyIndicator
        binding.mailSwipeRefresh.isEnabled = newState.swipeRefreshEnabled
        binding.mailSwipeRefresh.isRefreshing = newState.refreshing
    }
}