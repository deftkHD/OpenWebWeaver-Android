package de.deftk.openlonet.activities.feature

import android.annotation.SuppressLint
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.work.*
import de.deftk.lonet.api.LoNetClient
import de.deftk.lonet.api.implementation.OperatingScope
import de.deftk.lonet.api.implementation.feature.filestorage.RemoteFile
import de.deftk.lonet.api.implementation.feature.filestorage.session.SessionFile
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.filestorage.FileType
import de.deftk.lonet.api.model.feature.filestorage.IRemoteFile
import de.deftk.lonet.api.model.feature.filestorage.IRemoteFileProvider
import de.deftk.lonet.api.request.UserApiRequest
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.FileStorageFilesAdapter
import de.deftk.openlonet.databinding.ActivityFilesBinding
import de.deftk.openlonet.feature.filestorage.DownloadOpenWorker
import de.deftk.openlonet.feature.filestorage.DownloadSaveWorker
import de.deftk.openlonet.feature.filestorage.UploadWorker
import de.deftk.openlonet.utils.FileUtil
import de.deftk.openlonet.utils.getJsonExtra
import de.deftk.openlonet.utils.putJsonExtra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.io.File

class FilesActivity : AppCompatActivity() {

    companion object {
        const val FILE_PROVIDER_AUTHORITY = "de.deftk.openlonet.fileprovider"

        const val EXTRA_FOLDER = "de.deftk.openlonet.files.extra_folder"
        const val EXTRA_OPERATOR = "de.deftk.openlonet.files.extra_group"
    }

    private lateinit var binding: ActivityFilesBinding

    private lateinit var fileStorage: IRemoteFileProvider
    private lateinit var operator: OperatingScope

    private val requestedActivities = mutableMapOf<Int, RequestableAction>()
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val workManager by lazy { WorkManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra(EXTRA_OPERATOR)) {
            operator = intent.getJsonExtra<OperatingScope>(EXTRA_OPERATOR)!!
            if (intent.hasExtra(EXTRA_FOLDER))
                fileStorage = intent.getJsonExtra<RemoteFile>(EXTRA_FOLDER)!!
            else fileStorage = operator
        } else {
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = fileStorage.name

        binding.fileStorageSwipeRefresh.setOnRefreshListener {
            reloadFiles()
        }
        binding.fileList.setOnItemClickListener { _, _, position, _ ->
            val item = binding.fileList.getItemAtPosition(position) as IRemoteFileProvider
            if (item is RemoteFile) {
                if (item.type == FileType.FILE) {
                    if (item.effectiveRead() == true)
                        openFile(item)
                } else if (item.type == FileType.FOLDER) {
                    val intent = Intent(this, FilesActivity::class.java)
                    intent.putJsonExtra(EXTRA_FOLDER, item)
                    intent.putJsonExtra(EXTRA_OPERATOR, operator)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
        }

        val writeAccess = operator.effectiveRights.contains(Permission.FILES_WRITE) || operator.effectiveRights.contains(Permission.FILES_ADMIN)
        binding.fabUploadFile.isVisible = writeAccess
        binding.fabUploadFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "*/*"
            startActivityForResult(intent, getRequestCode(null, FileAction.UPLOAD_DOCUMENT))
        }

        registerForContextMenu(binding.fileList)
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
                (binding.fileList.adapter as Filterable).filter.filter(newText)
                return false
            }
        })

        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is AdapterView.AdapterContextMenuInfo) {
            val file = binding.fileList.adapter?.getItem(menuInfo.position) as RemoteFile
            if (file.effectiveRead() == true) {
                menuInflater.inflate(R.menu.filestorage_read_list_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filestorage_action_download -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val file = binding.fileList.adapter?.getItem(info.position) as RemoteFile
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.type = FileUtil.getMimeType(file.name)
                intent.putExtra(Intent.EXTRA_TITLE, file.name)
                startActivityForResult(intent, getRequestCode(file, FileAction.DOWNLOAD_SAVE))
                true
            }
            R.id.filestorage_action_open -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val file = binding.fileList.adapter?.getItem(info.position) as RemoteFile
                openFile(file)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val action = requestedActivities[requestCode]
        if (action != null && resultCode == RESULT_OK && data?.data != null) {
            when (action.action) {
                FileAction.DOWNLOAD_SAVE -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        withContext(Dispatchers.IO) {
                            check(action.target is RemoteFile) { "Invalid target; must be of type OnlineFile" }
                            val downloadUrl = action.target.getDownloadUrl(operator.getRequestContext(AuthStore.getApiContext())).url
                            withContext(Dispatchers.Main) {
                                doSaveDownload(data.data!!.toString(), downloadUrl, action.target)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@FilesActivity, R.string.download_finished, Toast.LENGTH_LONG).show()
                        }
                    }
                }
                FileAction.UPLOAD_DOCUMENT -> {
                    //TODO check if file exists
                    doUpload(data.data!!, queryFileName(data.data!!))
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    @SuppressLint("Recycle")
    private fun queryFileName(uri: Uri): String {
        var cursor: Cursor? = null
        var filename = "unknown.bin"
        try {
            cursor = contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null) ?: return "unknown.bin"

            if (cursor.moveToFirst()) {
                filename = cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
            }
        } finally {
            cursor?.close()
        }
        return filename
    }

    private fun openFile(file: RemoteFile) { //TODO replace with worker
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.IO) {
                val download = file.getDownloadUrl(context = operator.getRequestContext(AuthStore.getApiContext())).url
                val tempDir = File(cacheDir, "filestorage")
                if (!tempDir.isDirectory)
                    tempDir.mkdir()
                val tempFile = File(tempDir, file.name)
                withContext(Dispatchers.Main) {
                    doOpenDownload(tempFile.absolutePath, download, file)
                }
            }
        }
    }

    private fun getRequestCode(target: Any?, action: FileAction): Int {
        val id = requestedActivities.size
        requestedActivities[id] = RequestableAction(target, action)
        return id
    }

    private fun reloadFiles() {
        binding.fileList.adapter = null
        binding.fileEmpty.visibility = TextView.GONE
        CoroutineScope(Dispatchers.IO).launch {
            loadFiles()
        }
    }

    private suspend fun loadFiles() {
        try {
            val files = fileStorage.getFiles(context = operator.getRequestContext(AuthStore.getApiContext()))
            withContext(Dispatchers.Main) {
                binding.fileList.adapter = FileStorageFilesAdapter(this@FilesActivity, files, operator)
                binding.fileEmpty.isVisible = files.isEmpty()
                binding.progressFileStorage.visibility = ProgressBar.INVISIBLE
                binding.fileStorageSwipeRefresh.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.progressFileStorage.visibility = ProgressBar.INVISIBLE
                binding.fileStorageSwipeRefresh.isRefreshing = false
                Toast.makeText(
                    this@FilesActivity,
                    getString(R.string.request_failed_other).format(e.message ?: e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun doUpload(uri: Uri, fileName: String) {
        val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(
                UploadWorker.DATA_FILE_URI to uri.toString(),
                UploadWorker.DATA_FILE_NAME to fileName
            ))
            .build()
        workManager.getWorkInfoByIdLiveData(workRequest.id).observe(this) { workInfo ->
            if (workInfo != null) {
                //val progress = workInfo.progress.getInt(UploadWorker.ARGUMENT_PROGRESS, 0)

                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val sessionFile = LoNetClient.json.decodeFromString<SessionFile>(workInfo.outputData.getString(UploadWorker.DATA_SESSION_FILE)!!)
                        CoroutineScope(Dispatchers.IO).launch {
                            val newFile = fileStorage.importSessionFile(sessionFile, context = operator.getRequestContext(AuthStore.getApiContext()))
                            sessionFile.delete(AuthStore.getUserContext())

                            withContext(Dispatchers.Main) {
                                val adapter = (binding.fileList.adapter as FileStorageFilesAdapter)
                                adapter.insert(newFile, 0)
                                adapter.notifyDataSetChanged()
                                Toast.makeText(
                                    this@FilesActivity,
                                    R.string.upload_finished,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    WorkInfo.State.CANCELLED -> {
                        val request = UserApiRequest(AuthStore.getUserContext())

                        //TODO delete session file
                    }
                    WorkInfo.State.FAILED -> {

                    }
                    else -> { /* ignore */ }
                }
            }
        }
        workManager.enqueue(workRequest)
    }

    private fun doSaveDownload(destinationUrl: String, downloadUrl: String, file: IRemoteFile) {
        val workRequest = OneTimeWorkRequestBuilder<DownloadSaveWorker>()
            .setInputData(workDataOf(
                DownloadSaveWorker.DATA_DOWNLOAD_URL to downloadUrl,
                DownloadSaveWorker.DATA_DESTINATION_URI to destinationUrl,
                DownloadSaveWorker.DATA_FILE_NAME to file.name,
                DownloadSaveWorker.DATA_FILE_SIZE to file.getSize()
            ))
            .build()
        workManager.getWorkInfoByIdLiveData(workRequest.id).observe(this) { workInfo ->
            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> {
                    Toast.makeText(this, "YEET", Toast.LENGTH_LONG).show()
                }
                WorkInfo.State.CANCELLED -> {
                    //TODO delete local file
                }
                WorkInfo.State.FAILED -> {

                }
                else -> { /* ignore */ }
            }
        }
        workManager.enqueue(workRequest)
    }

    private fun doOpenDownload(destinationUrl: String, downloadUrl: String, file: IRemoteFile) {
        val workRequest = OneTimeWorkRequestBuilder<DownloadOpenWorker>()
            .setInputData(workDataOf(
                DownloadOpenWorker.DATA_DESTINATION_URI to destinationUrl,
                DownloadOpenWorker.DATA_DOWNLOAD_URL to downloadUrl,
                DownloadOpenWorker.DATA_FILE_NAME to file.name,
                DownloadOpenWorker.DATA_FILE_SIZE to file.getSize()
            ))
            .build()
        workManager.getWorkInfoByIdLiveData(workRequest.id).observe(this) { workInfo ->
            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> {
                    val fileUri = Uri.parse(workInfo.outputData.getString(DownloadOpenWorker.DATA_FILE_URI))

                    val mime = FileUtil.getMimeType(file.name)
                    val sendIntent = Intent(Intent.ACTION_SEND)
                    sendIntent.type = mime
                    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, normalizeFileName(file.name))
                    val viewIntent = Intent(Intent.ACTION_VIEW)
                    viewIntent.setDataAndType(fileUri, mime)
                    viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(Intent.createChooser(sendIntent, file.name).apply { putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent)) })
                }
                WorkInfo.State.CANCELLED -> {
                    //TODO delete file
                }
                WorkInfo.State.FAILED -> {
                    //TODO delete file
                }
                else -> { /* ignore */ }
            }
        }
        workManager.enqueue(workRequest)
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

    private data class RequestableAction(val target: Any?, val action: FileAction)

    private enum class FileAction {
        DOWNLOAD_SAVE,
        UPLOAD_DOCUMENT
    }

}