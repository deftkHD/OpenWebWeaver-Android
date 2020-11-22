package de.deftk.openlonet.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.mailbox.EmailFolder
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.abstract.FeatureFragment
import de.deftk.openlonet.abstract.IBackHandler
import de.deftk.openlonet.activities.feature.mail.MailsActivity
import de.deftk.openlonet.adapter.MailFolderAdapter
import de.deftk.openlonet.feature.AppFeature
import kotlinx.android.synthetic.main.fragment_mail.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MailFragment: FeatureFragment(AppFeature.FEATURE_MAIL), IBackHandler {

    //TODO context menu

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)

        val view = inflater.inflate(R.layout.fragment_mail, container, false)
        val list = view.findViewById<ListView>(R.id.mail_list)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.mail_swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            reloadEmailFolders()
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val item = list.getItemAtPosition(position)
            if (item is EmailFolder) {
                val intent = Intent(requireContext(), MailsActivity::class.java)
                intent.putExtra(MailsActivity.EXTRA_FOLDER, item)
                startActivity(intent)
            }
        }
        reloadEmailFolders()
        if (AuthStore.appUser.effectiveRights.contains(Permission.MAILBOX_ADMIN)) {
            val fab = view.findViewById<FloatingActionButton>(R.id.fab_mail_folder_add)
            fab.visibility = View.VISIBLE
            fab.setOnClickListener {
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
            }
        }
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                (mail_list.adapter as Filterable).filter.filter(newText)
                return false
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    private suspend fun createNewFolder(name: String) {
        withContext(Dispatchers.Main) {
            progress_mail.visibility = View.VISIBLE
        }
        try {
            AuthStore.appUser.addEmailFolder(name)
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
        mail_list?.adapter = null
        CoroutineScope(Dispatchers.IO).launch {
            loadEmailFolders()
        }
    }

    private suspend fun loadEmailFolders() {
        try {
            val folders = AuthStore.appUser.getEmailFolders()
            withContext(Dispatchers.Main) {
                mail_list?.adapter = MailFolderAdapter(requireContext(), folders)
                mail_empty?.isVisible = folders.isEmpty()
                progress_mail?.visibility = ProgressBar.GONE
                mail_swipe_refresh?.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                progress_mail?.visibility = ProgressBar.GONE
                mail_swipe_refresh?.isRefreshing = false
                Toast.makeText(
                    context,
                    getString(R.string.request_failed_other).format(e.message ?: e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun getTitle(): String {
        return getString(R.string.mail)
    }

}