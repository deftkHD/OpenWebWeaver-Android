package de.deftk.openlonet.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import de.deftk.lonet.api.implementation.feature.mailbox.Email
import de.deftk.lonet.api.implementation.feature.mailbox.EmailFolder
import de.deftk.lonet.api.model.Permission
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.abstract.FeatureFragment
import de.deftk.openlonet.abstract.IBackHandler
import de.deftk.openlonet.activities.feature.mail.ReadMailActivity
import de.deftk.openlonet.activities.feature.mail.WriteMailActivity
import de.deftk.openlonet.adapter.MailAdapter
import de.deftk.openlonet.adapter.MailFolderAdapter
import de.deftk.openlonet.databinding.FragmentMailBinding
import de.deftk.openlonet.feature.AppFeature
import de.deftk.openlonet.utils.putJsonExtra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MailFragment: FeatureFragment(AppFeature.FEATURE_MAIL), IBackHandler {

    //TODO context menu

    companion object {
        const val REQUEST_CODE_WRITE_MAIL = 1
    }

    private lateinit var binding: FragmentMailBinding
    private lateinit var toolbarSpinner: Spinner
    private var currentFolder: EmailFolder? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMailBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        toolbarSpinner = requireActivity().findViewById(R.id.toolbar_spinner)
        toolbarSpinner.visibility = View.VISIBLE
        toolbarSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val folder = toolbarSpinner.getItemAtPosition(position) as EmailFolder
                currentFolder = folder
                reloadEmails()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.mailSwipeRefresh.setOnRefreshListener {
            reloadEmails()
        }
        binding.mailList.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(requireContext(), ReadMailActivity::class.java)
            intent.putJsonExtra(ReadMailActivity.EXTRA_MAIL, binding.mailList.getItemAtPosition(position) as Email)
            intent.putJsonExtra(ReadMailActivity.EXTRA_FOLDER, currentFolder)
            startActivity(intent)
        }
        if (AuthStore.getApiUser().effectiveRights.contains(Permission.MAILBOX_ADMIN)) {
            binding.fabMailAdd.visibility = View.VISIBLE
            binding.fabMailAdd.setOnClickListener {
                val intent = Intent(requireContext(), WriteMailActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_WRITE_MAIL)
            }
        }

        reloadEmailFolders()
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
                (binding.mailList.adapter as Filterable).filter.filter(newText)
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
                CoroutineScope(Dispatchers.IO).launch {
                    createNewFolder(input.text.toString())
                }
            }
            builder.setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
            return true
        } else return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun onDestroy() {
        toolbarSpinner.visibility = View.GONE
        super.onDestroy()
    }

    private suspend fun createNewFolder(name: String) {
        withContext(Dispatchers.Main) {
            binding.progressMail.visibility = View.VISIBLE
        }
        try {
            AuthStore.getApiUser().addEmailFolder(name, AuthStore.getUserContext())
            withContext(Dispatchers.Main) {
                reloadEmailFolders()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                getString(R.string.request_failed_other).format(e.message ?: e),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun reloadEmailFolders() {
        toolbarSpinner.adapter = null
        CoroutineScope(Dispatchers.IO).launch {
            loadEmailFolders()
        }
    }

    private suspend fun loadEmailFolders() {
        try {
            val folders = AuthStore.getApiUser().getEmailFolders(AuthStore.getUserContext())
            withContext(Dispatchers.Main) {
                toolbarSpinner.adapter = MailFolderAdapter(requireContext(), folders)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    getString(R.string.request_failed_other).format(e.message ?: e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun reloadEmails() {
        binding.mailList.adapter = null
        CoroutineScope(Dispatchers.IO).launch {
            loadEmails()
        }
    }

    private suspend fun loadEmails() {
        try {
            val emails = currentFolder?.getEmails(context = AuthStore.getUserContext()) ?: emptyList()
            withContext(Dispatchers.Main) {
                binding.mailList.adapter = MailAdapter(requireContext(), emails, currentFolder!!)
                binding.mailEmpty.isVisible = emails.isEmpty()
                binding.progressMail.visibility = ProgressBar.GONE
                binding.mailSwipeRefresh.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.progressMail.visibility = ProgressBar.GONE
                binding.mailSwipeRefresh.isRefreshing = false
                Toast.makeText(
                    context,
                    getString(R.string.request_failed_other).format(e.message ?: e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun getTitle() = ""

}