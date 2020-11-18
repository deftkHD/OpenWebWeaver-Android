package de.deftk.lonet.mobile.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.model.Group
import de.deftk.lonet.api.model.User
import de.deftk.lonet.api.model.abstract.AbstractOperator
import de.deftk.lonet.api.model.feature.mailbox.Email
import de.deftk.lonet.api.model.feature.mailbox.EmailFolder
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.FeatureFragment
import de.deftk.lonet.mobile.abstract.IBackHandler
import de.deftk.lonet.mobile.activities.feature.mail.MailsActivity
import de.deftk.lonet.mobile.activities.feature.mail.ReadMailActivity
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        /* mail_write_mail?.isEnabled = true
        mail_write_mail?.setOnClickListener {
            val intent = Intent(context, WriteMailActivity::class.java)
            context?.startActivity(intent)
        } */
        return view
    }

    override fun onBackPressed(): Boolean {
        return false
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

    override fun getTitle(): String {
        return getString(R.string.mail)
    }

}