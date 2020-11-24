package de.deftk.openlonet.activities.feature

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import de.deftk.lonet.api.model.abstract.AbstractOperator
import de.deftk.lonet.api.model.feature.abstract.IFilePrimitive
import de.deftk.lonet.api.model.feature.files.OnlineFile
import de.deftk.openlonet.BuildConfig
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.FileStorageFilesAdapter
import de.deftk.openlonet.utils.FileUtil
import kotlinx.android.synthetic.main.activity_files.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilesActivity : AppCompatActivity() {

    //TODO show preview image if possible

    companion object {
        const val EXTRA_FOLDER = "de.deftk.openlonet.files.extra_folder"
    }

    private lateinit var fileStorage: IFilePrimitive

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    private val onCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)
                openDownload(context, downloadId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files)
        registerReceiver(onCompleteReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        val extraFolder = intent.getSerializableExtra(EXTRA_FOLDER) as? IFilePrimitive?
        if (extraFolder != null) {
            fileStorage = extraFolder
        } else {
            finish()
            return
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = when (extraFolder) {
            is AbstractOperator -> extraFolder.getName()
            is OnlineFile -> extraFolder.name
            else -> ""
        }

        file_storage_swipe_refresh.setOnRefreshListener {
            reloadFiles()
        }
        file_list.setOnItemClickListener { _, _, position, _ ->
            val item = file_list.getItemAtPosition(position) as IFilePrimitive
            if (item is OnlineFile) {
                if (item.type == OnlineFile.FileType.FILE) {
                    CoroutineScope(Dispatchers.IO).launch {
                        downloadFile(item)
                    }
                } else if (item.type == OnlineFile.FileType.FOLDER) {
                    val i = Intent(this, FilesActivity::class.java)
                    i.putExtra(EXTRA_FOLDER, item)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(i)
                }
            }
        }

        reloadFiles()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                (file_list.adapter as Filterable).filter.filter(newText)
                return false
            }
        })

        return true
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        unregisterReceiver(onCompleteReceiver)
        super.onDestroy()
    }

    private fun reloadFiles() {
        file_list.adapter = null
        file_empty.visibility = TextView.GONE
        CoroutineScope(Dispatchers.IO).launch {
            loadFiles()
        }
    }

    private suspend fun loadFiles() {
        try {
            val files = fileStorage.getFiles()
            withContext(Dispatchers.Main) {
                file_list?.adapter = FileStorageFilesAdapter(this@FilesActivity, files)
                file_empty.isVisible = files.isEmpty()
                progress_file_storage?.visibility = ProgressBar.INVISIBLE
                file_storage_swipe_refresh?.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                progress_file_storage?.visibility = ProgressBar.INVISIBLE
                file_storage_swipe_refresh?.isRefreshing = false
                Toast.makeText(
                    this@FilesActivity,
                    getString(R.string.request_failed_other).format(e.message ?: e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    //TODO scoped storage
    private suspend fun downloadFile(file: OnlineFile) {
        try {
            val downloadUrl = file.getTempDownloadUrl().downloadUrl
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
            val fileName = file.name.replace("[^0-9a-zA-Z_.]".toRegex(), "")
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
            request.setTitle("Download")
            request.setDescription(file.name)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "temp/$fileName"
            )
            request.setMimeType(FileUtil.getMimeType(file.name))

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@FilesActivity,
                    getString(R.string.request_failed_other).format(e.message ?: e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openDownload(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query()
        query.setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            val downloadLocalUri =
                cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
            val downloadMimeType =
                cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE))
            if ((downloadStatus == DownloadManager.STATUS_SUCCESSFUL) && downloadLocalUri != null) {
                openDownload(context, Uri.parse(downloadLocalUri), downloadMimeType)
            }
        }
        cursor.close()
    }

    private fun openDownload(context: Context, fileUri: Uri?, mimeType: String?) {
        if (fileUri != null) {
            val intent = Intent(Intent.ACTION_SEND)
            val sharedUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", fileUri.toFile())
            intent.type = mimeType
            intent.putExtra(Intent.EXTRA_STREAM, sharedUri)
            intent.putExtra(Intent.EXTRA_SUBJECT, normalizeFileName(sharedUri.lastPathSegment ?: "imported"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    String.format(
                        context.getText(R.string.download_open_failed).toString(),
                        e.message ?: e
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun normalizeFileName(name: String): String {
        return if (preferences.getBoolean("file_storage_correct_file_names", false)) {
            if (name.contains('.')) {
                name.substring(0, name.lastIndexOf('.')).replace("_", " ")
            } else {
                name.replace("_", " ")
            }
        } else {
            name
        }
    }

}