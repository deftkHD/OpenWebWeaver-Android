package de.deftk.openww.android.fragments.feature.mail

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.AdapterView
import android.widget.EditText
import android.widget.SearchView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.api.implementation.feature.mailbox.EmailFolder
import de.deftk.openww.api.model.Permission
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.MailFolderAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentMailBinding
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.MailboxViewModel
import de.deftk.openww.android.viewmodel.UserViewModel

class MailFragment: Fragment() {

    //TODO context menu

    private val args: MailFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val mailboxViewModel: MailboxViewModel by activityViewModels()
    private val navController: NavController by lazy { findNavController() }

    private lateinit var binding: FragmentMailBinding
    private lateinit var toolbarSpinner: Spinner

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMailBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()

        toolbarSpinner = requireActivity().findViewById(R.id.toolbar_spinner)
        toolbarSpinner.isVisible = true
        toolbarSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val folder = toolbarSpinner.getItemAtPosition(position) as EmailFolder
                if (folder.id != mailboxViewModel.currentFolder.value?.id) {
                    userViewModel.apiContext.value?.also { apiContext ->
                        mailboxViewModel.selectFolder(folder, apiContext)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val adapter = de.deftk.openww.android.adapter.recycler.MailAdapter()
        binding.mailList.adapter = adapter
        binding.mailList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        mailboxViewModel.currentMails.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value.map { it to mailboxViewModel.currentFolder.value!! })
                binding.mailEmpty.isVisible = response.value.isEmpty()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_emails_failed, response.exception, requireContext())
            }
            binding.progressMail.isVisible = false
            binding.mailSwipeRefresh.isRefreshing = false
        }

        binding.mailSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                mailboxViewModel.cleanCache()
                mailboxViewModel.loadFolders(apiContext)
            }
        }

        mailboxViewModel.foldersResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                val newAdapter = MailFolderAdapter(requireContext(), response.value)
                if ((toolbarSpinner.adapter as? MailFolderAdapter?)?.elements != newAdapter.elements) {
                    toolbarSpinner.adapter = newAdapter
                    userViewModel.apiContext.value?.also { apiContext ->
                        mailboxViewModel.selectFolder(response.value.firstOrNull { it.id == args.folderId } ?: response.value.first { it.isInbox }, apiContext)
                    }
                } else {
                    binding.progressMail.isVisible = false
                    binding.mailSwipeRefresh.isRefreshing = false
                }
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_folders_failed, response.exception, requireContext())
                toolbarSpinner.adapter = null
                binding.progressMail.isVisible = false
                binding.mailSwipeRefresh.isRefreshing = false
            }
        }

        if (userViewModel.apiContext.value?.getUser()?.effectiveRights?.contains(Permission.MAILBOX_ADMIN) == true) {
            binding.fabMailAdd.visibility = View.VISIBLE
            binding.fabMailAdd.setOnClickListener {
                navController.navigate(MailFragmentDirections.actionMailFragmentToWriteMailFragment())
                toolbarSpinner.isVisible = false
                toolbarSpinner.adapter = null
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                mailboxViewModel.cleanCache()
                mailboxViewModel.loadFolders(apiContext)
            } else {
                binding.fabMailAdd.isVisible = false
                binding.mailEmpty.isVisible = false
                toolbarSpinner.adapter = null
                adapter.submitList(emptyList())
                binding.progressMail.isVisible = true
            }
        }

        mailboxViewModel.folderPostResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                mailboxViewModel.resetPostResponse() // mark as handled

            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_save_changes_failed, response.exception, requireContext())
            }
        }

        setHasOptionsMenu(true)
        registerForContextMenu(binding.mailList)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()

        // search menu
        inflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                //TODO search
                return false
            }
        })

        // utility menu
        inflater.inflate(R.menu.mail_list_menu, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.mail_list_menu_add_folder) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(R.string.create_new_folder)

            val input = EditText(requireContext())
            input.hint = getString(R.string.name)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            builder.setPositiveButton(R.string.confirm) { _, _ ->
                userViewModel.apiContext.value?.apply {
                    mailboxViewModel.addFolder(input.text.toString(), this)
                }
            }
            builder.setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
            return true
        } else return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        toolbarSpinner.isVisible = true
        super.onResume()
    }

    override fun onStop() {
        toolbarSpinner.isVisible = false
        super.onStop()
    }

    override fun onDestroy() {
        toolbarSpinner.adapter = null
        super.onDestroy()
    }

}