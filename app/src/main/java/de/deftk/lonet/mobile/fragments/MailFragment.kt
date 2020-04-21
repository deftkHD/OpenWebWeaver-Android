package de.deftk.lonet.mobile.fragments

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

class MailFragment: FeatureFragment(AppFeature.FEATURE_MAIL), IBackHandler {

    //TODO filter
    //TODO context menu
    //TODO write emails

    private var currentDirectory: EmailFolder? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (currentDirectory == null)
            navigate(null)
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
        if (folder == null) {
            (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.email)
            LoadEmailFoldersTask().execute()
        } else {
            (activity as AppCompatActivity).supportActionBar?.title = MailFolderAdapter.getDefaultFolderTranslation(context ?: error("Oops, no context?"), folder)
            LoadEmailsTask().execute(folder)
        }
    }

    private inner class LoadEmailsTask: AsyncTask<EmailFolder, Void, List<Email>?>() {
        override fun doInBackground(vararg params: EmailFolder): List<Email>? {
            return try {
                params[0].getEmails(AuthStore.appUser, true) // always want to get the newest mails
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: List<Email>?) {
            if (result != null) {
                progress_mail?.visibility = ProgressBar.INVISIBLE
                mail_list?.adapter = MailAdapter(context ?: error("Oops, no context?"), result)
                mail_swipe_refresh?.isRefreshing = false
            } else {
                Toast.makeText(context, getString(R.string.request_failed_other).format("No details"), Toast.LENGTH_LONG).show()
            }
        }
    }

    private inner class LoadEmailFoldersTask: AsyncTask<Void, Void, List<EmailFolder>?>() {
        override fun doInBackground(vararg params: Void?): List<EmailFolder>? {
            return try {
                AuthStore.appUser.getEmailFolders(true)
            }  catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: List<EmailFolder>?) {
            if (result != null) {
                progress_mail?.visibility = ProgressBar.INVISIBLE
                mail_list?.adapter = MailFolderAdapter(context ?: error("Oops, no context?"), result)
                mail_swipe_refresh?.isRefreshing = false
            } else {
                Toast.makeText(context, getString(R.string.request_failed_other).format("No details"), Toast.LENGTH_LONG).show()
            }
        }
    }

}