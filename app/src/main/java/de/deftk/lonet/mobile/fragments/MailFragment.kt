package de.deftk.lonet.mobile.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.model.feature.mailbox.Email
import de.deftk.lonet.api.model.feature.mailbox.EmailFolder
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.FeatureFragment
import de.deftk.lonet.mobile.abstract.IBackHandler
import de.deftk.lonet.mobile.activities.feature.ReadMailActivity
import de.deftk.lonet.mobile.adapter.MailAdapter
import de.deftk.lonet.mobile.adapter.MailFolderAdapter
import de.deftk.lonet.mobile.feature.AppFeature
import kotlinx.android.synthetic.main.fragment_mail.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MailFragment: FeatureFragment(AppFeature.FEATURE_MAIL), IBackHandler {

    //TODO filter
    //TODO context menu
    //TODO write emails

    companion object {
        private const val SAVE_CURRENT_DIRECTORY = "de.deftk.lonet.mobile.mail.current_directory"
    }

    private var currentDirectory: EmailFolder? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (savedInstanceState != null) {
            currentDirectory = savedInstanceState.getSerializable(SAVE_CURRENT_DIRECTORY) as EmailFolder?
        }
        navigate(currentDirectory)
        val view = inflater.inflate(R.layout.fragment_mail, container, false)
        val list = view.findViewById<ListView>(R.id.mail_list)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.mail_swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            list.adapter = null
            navigate(currentDirectory)
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val item = list.getItemAtPosition(position)
            if (item is EmailFolder) {
                navigate(item)
            } else {
                item as Email
                val intent = Intent(context, ReadMailActivity::class.java)
                intent.putExtra(ReadMailActivity.EXTRA_MAIL, item)
                startActivity(intent)
            }
        }
        /* mail_write_mail?.isEnabled = true
        mail_write_mail?.setOnClickListener {
            val intent = Intent(context, WriteMailActivity::class.java)
            context?.startActivity(intent)
        } */
        return view
    }

    override fun onBackPressed(): Boolean {
        if (currentDirectory != null) {
            navigate(null)
            return true
        }
        return false
    }

    private fun navigate(folder: EmailFolder?) {
        currentDirectory = folder
        mail_list?.adapter = null
        (activity as AppCompatActivity?)?.supportActionBar?.title = getTitle()
        CoroutineScope(Dispatchers.IO).launch {
            if (folder == null) {
                loadEmailFolders()
            } else {
                loadEmails(folder)
            }
        }
    }

    private suspend fun loadEmailFolders() {
        try {
            val folders = AuthStore.appUser.getEmailFolders()
            withContext(Dispatchers.Main) {
                mail_list?.adapter = MailFolderAdapter(requireContext(), folders)
                mail_empty?.isVisible = folders.isEmpty()
                progress_mail?.visibility = ProgressBar.INVISIBLE
                mail_swipe_refresh?.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                progress_mail?.visibility = ProgressBar.INVISIBLE
                mail_swipe_refresh?.isRefreshing = false
                Toast.makeText(context, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun loadEmails(folder: EmailFolder) {
        try {
            val emails = folder.getEmails()
            withContext(Dispatchers.Main) {
                mail_list?.adapter = MailAdapter(requireContext(), emails)
                mail_empty?.isVisible = emails.isEmpty()
                progress_mail?.visibility = ProgressBar.INVISIBLE
                mail_swipe_refresh?.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                progress_mail?.visibility = ProgressBar.INVISIBLE
                mail_swipe_refresh?.isRefreshing = false
                Toast.makeText(context, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(SAVE_CURRENT_DIRECTORY, currentDirectory)
    }

    override fun getTitle(): String {
        return if (currentDirectory == null) getString(R.string.mail)
        else MailFolderAdapter.getDefaultFolderTranslation(requireContext(), currentDirectory!!)
    }

}