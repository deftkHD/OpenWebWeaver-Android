package de.deftk.lonet.mobile.fragments

import android.content.Intent
import android.net.Uri
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
import de.deftk.lonet.api.model.Group
import de.deftk.lonet.api.model.feature.Quota
import de.deftk.lonet.api.model.feature.abstract.IFilePrimitive
import de.deftk.lonet.api.model.feature.files.OnlineFile
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.FeatureFragment
import de.deftk.lonet.mobile.abstract.IBackHandler
import de.deftk.lonet.mobile.adapter.FileStorageAdapter
import de.deftk.lonet.mobile.adapter.FileStorageFilesAdapter
import de.deftk.lonet.mobile.feature.AppFeature
import de.deftk.lonet.mobile.utils.FileUtil
import kotlinx.android.synthetic.main.fragment_file_storage.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*

class FileStorageFragment: FeatureFragment(AppFeature.FEATURE_FILE_STORAGE), IBackHandler {

    private val history = Stack<IFilePrimitive>()
    private var currentGroup: Group? = null

    companion object {
        private const val SAVE_HISTORY = "de.deftk.lonet.mobile.files.history"
        private const val SAVE_GROUP = "de.deftk.lonet.mobile.files.group"
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
            (savedInstanceState.getSerializable(SAVE_HISTORY) as? Stack<*>)?.forEach {
                this.history.push(it as IFilePrimitive)
            }
            currentGroup = savedInstanceState.getSerializable(SAVE_GROUP) as? Group
        }

        val view = inflater.inflate(R.layout.fragment_file_storage, container, false)
        val list = view.findViewById<ListView>(R.id.file_list)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.file_storage_swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            list.adapter = null
            CoroutineScope(Dispatchers.IO).launch {
                loadFiles(history.peek())
            }
        }
        list.setOnItemClickListener { _, _, position, _ ->
            when (val item = list.getItemAtPosition(position)) {
                is OnlineFile -> {
                    if (item.type == OnlineFile.FileType.FOLDER) {
                        navigate(item)
                    } else {
                        CoroutineScope(Dispatchers.IO).launch {
                            downloadOpenFile(item)
                        }
                        Toast.makeText(context, getString(R.string.download_started), Toast.LENGTH_SHORT).show()
                    }
                }
                is Group -> {
                    navigate(item)
                }
            }

        }
        list.setOnItemLongClickListener { _, _, position, _ ->
            val item = list.getItemAtPosition(position)
            //TODO show context menu
            true
        }
        navigate(if (history.isNotEmpty()) history.pop() else currentGroup)
        return view
    }

    private fun navigate(directory: Any?) {
        file_list?.adapter = null
        progress_file_storage?.visibility = ProgressBar.VISIBLE
        when (directory) {
            is IFilePrimitive -> {
                if (directory is Group)
                    currentGroup = directory
                else
                    history.push(directory)
                CoroutineScope(Dispatchers.IO).launch {
                    loadFiles(directory)
                }
            }
            null -> {
                history.clear()
                currentGroup = null
                CoroutineScope(Dispatchers.IO).launch {
                    loadFiles(null)
                }
            }
        }
        (activity as AppCompatActivity).supportActionBar?.title = getTitle()
    }

    override fun onBackPressed(): Boolean {
        return when {
            history.isNotEmpty() -> {
                history.pop()
                navigate(if (history.isEmpty()) currentGroup else history.pop()) // item will be pushed again by navigate() //FIXME maybe not that nice to reset every scroll index on going back
                true
            }
            currentGroup != null -> {
                navigate(null) // show groups
                true
            }
            else -> false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(SAVE_HISTORY, history)
        if (currentGroup != null)
            outState.putSerializable(SAVE_GROUP, currentGroup)
    }

    override fun getTitle(): String {
        val currentDirectory = if(history.isEmpty()) null else history.peek()
        return if (currentDirectory is OnlineFile)
            currentDirectory.name
        else getString(R.string.file_storage)
    }

    private suspend fun loadFiles(folder: IFilePrimitive?) {
        try {
            if (folder != null) {
                val files = folder.getFiles()
                withContext(Dispatchers.Main) {
                    file_list?.adapter = FileStorageFilesAdapter(requireContext(), files)
                    file_empty.isVisible = files.isEmpty()
                    progress_file_storage?.visibility = ProgressBar.INVISIBLE
                    file_storage_swipe_refresh?.isRefreshing = false
                }
            } else {
                val groups = AuthStore.appUser.getContext().getGroups().map {
                    try {
                        Pair(it, it.getFileStorageState().second)
                } catch (e: Exception) {
                        Pair(it, Quota(0, 0, 0, 0, -1, -1))
                    }
                }
                withContext(Dispatchers.Main) {
                    file_list?.adapter = FileStorageAdapter(requireContext(), groups.toMap())
                    file_empty.isVisible = groups.isEmpty()
                    progress_file_storage?.visibility = ProgressBar.INVISIBLE
                    file_storage_swipe_refresh?.isRefreshing = false
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                progress_file_storage?.visibility = ProgressBar.INVISIBLE
                file_storage_swipe_refresh?.isRefreshing = false
                Toast.makeText(context, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
            }
        }
    }

    //TODO progress dialog
    private suspend fun downloadOpenFile(file: OnlineFile) {
        try {
            val url = URL(file.getTempDownloadUrl().downloadUrl)
            val targetFile = File(context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: error("no download directory?"), file.name.replace("/", "_"))
            if (targetFile.exists()) targetFile.delete()
            url.openStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, getString(R.string.download_finished), Toast.LENGTH_SHORT).show()
                val intent = Intent(Intent.ACTION_VIEW)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), requireContext().packageName + ".provider", targetFile)
                    intent.setDataAndType(uri, FileUtil.getMimeType(uri.toString()))
                } else {
                    val uri = Uri.fromFile(targetFile)
                    intent.setDataAndType(uri, FileUtil.getMimeType(uri.toString()))
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
            }
        }
    }

}