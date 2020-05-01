package de.deftk.lonet.mobile.fragments

import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.model.feature.abstract.IFilePrimitive
import de.deftk.lonet.api.model.feature.files.OnlineFile
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.FeatureFragment
import de.deftk.lonet.mobile.abstract.IBackHandler
import de.deftk.lonet.mobile.adapter.FileStorageAdapter
import de.deftk.lonet.mobile.feature.AppFeature
import de.deftk.lonet.mobile.feature.RootFileProvider
import de.deftk.lonet.mobile.utils.FileUtil
import kotlinx.android.synthetic.main.fragment_file_storage.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*

class FileStorageFragment: FeatureFragment(AppFeature.FEATURE_FILE_STORAGE), IBackHandler {

    private val history = Stack<IFilePrimitive>()

    companion object {
        private const val SAVE_HISTORY = "de.deftk.lonet.mobile.files.history"
        const val ARGUMENT_GROUP = "de.deftk.lonet.mobile.files.argument_group"
        const val ARGUMENT_FILE_ID = "de.deftk.lonet.mobile.files.argument_file_id"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (arguments != null) {
            val group = arguments?.getString(ARGUMENT_GROUP)
            val file = arguments?.getString(ARGUMENT_FILE_ID)
            //TODO jump to given location
        }

        if (savedInstanceState != null) {
            val history = savedInstanceState.getSerializable(SAVE_HISTORY) as Stack<*>
            history.forEach {
                this.history.push(it as IFilePrimitive)
            }
            println()
        }

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
                Toast.makeText(context, getString(R.string.download_started), Toast.LENGTH_SHORT).show()
            }
        }
        list.setOnItemLongClickListener { _, _, position, _ ->
            val item = list.getItemAtPosition(position)
            //TODO show context menu
            true
        }
        if (history.isEmpty())
            history.push(RootFileProvider())
        navigate(history.pop())
        return view
    }

    private fun navigate(directory: IFilePrimitive) {
        file_list?.adapter = null
        progress_file_storage?.visibility = ProgressBar.VISIBLE
        DirectoryLoadingTask().execute(directory)
        history.push(directory)
        (activity as AppCompatActivity).supportActionBar?.title = getTitle()
    }

    override fun onBackPressed(): Boolean {
        if (history.size > 1) {
            history.pop()
            navigate(history.pop()) // item will be pushed again by navigate() //FIXME maybe not that nice to reset every scroll index on going back
            return true
        }
        return false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(SAVE_HISTORY, history)
    }

    override fun getTitle(): String {
        val currentDirectory = if(history.isEmpty()) null else history.peek()
        return if (currentDirectory is OnlineFile)
            currentDirectory.name
        else getString(R.string.file_storage)
    }

    private inner class DirectoryLoadingTask: AsyncTask<IFilePrimitive, Void, List<OnlineFile>?>() {

        override fun doInBackground(vararg params: IFilePrimitive): List<OnlineFile>? {
            return try {
                params[0].getFileStorageFiles(true) // don't want to cache file request here
                    .sortedByDescending { it.type }
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: List<OnlineFile>?) {
            progress_file_storage?.visibility = ProgressBar.INVISIBLE
            file_storage_swipe_refresh?.isRefreshing = false
            if (context != null) {
                if (result != null) {
                    file_list?.adapter = FileStorageAdapter(context!!, result)
                    file_empty.isVisible = result.isEmpty()
                } else {
                    Toast.makeText(context, getString(R.string.request_failed_other).format("No details"), Toast.LENGTH_LONG).show()
                }
            }
        }

    }

    //TODO progress dialog
    private inner class FileDownloadOpenTask: AsyncTask<OnlineFile, Void, File?>() {

        override fun doInBackground(vararg params: OnlineFile): File? {
            return try {
                 val url = URL(params[0].getTmpDownloadUrl(true).downloadUrl)
                val targetFile = File(context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: error("no download directory?"), params[0].name.replace("/", "_"))
                if (targetFile.exists()) targetFile.delete()
                url.openStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: File?) {
            if (context != null) {
                if (result != null) {
                    Toast.makeText(context, getString(R.string.download_finished), Toast.LENGTH_SHORT).show()
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                        val uri = androidx.core.content.FileProvider.getUriForFile(context!!, context!!.packageName + ".provider", result)
                        intent.setDataAndType(uri, FileUtil.getMimeType(uri.toString()))
                    } else {
                        val uri = Uri.fromFile(result)
                        intent.setDataAndType(uri, FileUtil.getMimeType(uri.toString()))
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(context, getString(R.string.request_failed_other).format("No details"), Toast.LENGTH_LONG).show()
                }
            }
        }

    }

}