package de.deftk.lonet.mobile.fragments

import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.model.feature.files.FileProvider
import de.deftk.lonet.api.model.feature.files.OnlineFile
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.FeatureFragment
import de.deftk.lonet.mobile.abstract.IBackHandler
import de.deftk.lonet.mobile.adapter.FileStorageAdapter
import de.deftk.lonet.mobile.feature.AppFeature
import de.deftk.lonet.mobile.feature.RootFileProvider
import kotlinx.android.synthetic.main.fragment_file_storage.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.channels.Channels
import java.util.*

class FileStorageFragment(): FeatureFragment(AppFeature.FEATURE_FILE_STORAGE), IBackHandler {

    private val history = Stack<FileProvider>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_file_storage, container, false)
        val list = view.findViewById<ListView>(R.id.file_list)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.file_storage_swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            list.adapter = null
            DirectoryLoadingTask().execute(history.peek())
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val item = list.getItemAtPosition(position) as OnlineFile
            if (item.type == OnlineFile.FileType.FOLDER) {
                navigate(item)
            } else {
                FileDownloadOpenTask().execute(item)
                Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
            }
        }
        list.setOnItemLongClickListener { _, _, position, _ ->
            val item = list.getItemAtPosition(position)
            //TODO show context menu
            true
        }
        if (history.isEmpty())
            navigate(RootFileProvider())
        return view
    }

    private fun navigate(directory: FileProvider) {
        progress_file_storage?.visibility = ProgressBar.VISIBLE
        DirectoryLoadingTask().execute(directory)
        history.push(directory)
    }

    override fun onBackPressed(): Boolean {
        if (history.size > 1) {
            history.pop()
            navigate(history.pop()) // item will be pushed again by navigate() //FIXME maybe not that nice to reset every scroll index on going back
            return true
        }
        return false
    }

    private inner class DirectoryLoadingTask: AsyncTask<FileProvider, Void, List<OnlineFile>>() {

        override fun doInBackground(vararg params: FileProvider): List<OnlineFile> {
            return params[0].getFiles(AuthStore.appUser.sessionId, true) // don't want to cache file request here
                .sortedByDescending { it.type }
        }

        override fun onPostExecute(result: List<OnlineFile>) {
            progress_file_storage?.visibility = ProgressBar.INVISIBLE
            file_list?.adapter = FileStorageAdapter(context ?: error("Oops, no context?"), result)
            file_empty.isVisible = result.isEmpty()
            file_storage_swipe_refresh?.isRefreshing = false
        }

    }

    //TODO progress dialog
    private inner class FileDownloadOpenTask: AsyncTask<OnlineFile, Void, File>() {

        //FIXME please fix me
        override fun doInBackground(vararg params: OnlineFile): File {
            val url = URL(params[0].downloadUrl ?: error("Server did not return download url!"))
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            val targetFile = File(context!!.cacheDir, params[0].name.replace("/", "_"))
            if (targetFile.exists()) targetFile.delete()
            if (targetFile.parentFile?.exists() != true)
                targetFile.parentFile?.mkdirs()
            targetFile.deleteOnExit()
            val fout = FileOutputStream(targetFile)
            fout.channel.transferFrom(Channels.newChannel(connection.inputStream), 0, connection.contentLength.toLong())
            return targetFile
        }

        override fun onPostExecute(result: File) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                val a = context!!.packageManager
                intent.setDataAndType(androidx.core.content.FileProvider.getUriForFile(context!!, context!!.packageName + ".provider", result), "application/vnd.android.package-archive")
            } else {
                intent.setDataAndType(Uri.fromFile(result), "application/vnd.android.package-archive")
            }
            startActivity(intent)
        }

    }

}