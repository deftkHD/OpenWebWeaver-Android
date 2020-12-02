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
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.abstract.AbstractOperator
import de.deftk.lonet.api.model.feature.abstract.IFilePrimitive
import de.deftk.lonet.api.model.feature.files.OnlineFile
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.FileStorageFilesAdapter
import de.deftk.openlonet.utils.FileUtil
import kotlinx.android.synthetic.main.activity_files.*
import kotlinx.android.synthetic.main.list_item_file.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class FilesActivity : AppCompatActivity() {

    companion object {
        private const val FILE_PROVIDER_AUTHORITY = "de.deftk.openlonet.fileprovider"

        const val EXTRA_FOLDER = "de.deftk.openlonet.files.extra_folder"
    }

    private lateinit var fileStorage: IFilePrimitive

    private val requestedActivities = mutableMapOf<Int, RequestableAction>()
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files)

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
        supportActionBar?.title = extraFolder.getName()

        file_storage_swipe_refresh.setOnRefreshListener {
            reloadFiles()
        }
        file_list.setOnItemClickListener { _, _, position, _ ->
            val item = file_list.getItemAtPosition(position) as IFilePrimitive
            if (item is OnlineFile) {
                if (item.type == OnlineFile.FileType.FILE) {
                    if (item.effectiveRead == true)
                        openFile(item)
                } else if (item.type == OnlineFile.FileType.FOLDER) {
                    val i = Intent(this, FilesActivity::class.java)
                    i.putExtra(EXTRA_FOLDER, item)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(i)
                }
            }
        }

        val operator = (if (extraFolder is AbstractOperator) extraFolder else if (extraFolder is OnlineFile) extraFolder.operator else null)!!
        val writeAccess = operator.effectiveRights.contains(Permission.FILES_WRITE) || operator.effectiveRights.contains(Permission.FILES_ADMIN)
        fab_upload_file.isVisible = writeAccess
        fab_upload_file.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "*/*"
            startActivityForResult(intent, getRequestCode(null, FileAction.UPLOAD_DOCUMENT))
        }

        registerForContextMenu(file_list)
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

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is AdapterView.AdapterContextMenuInfo) {
            val file = file_list?.adapter?.getItem(menuInfo.position) as OnlineFile
            if (file.effectiveRead == true) {
                menuInflater.inflate(R.menu.filestorage_read_list_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filestorage_action_download -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val file = file_list?.adapter?.getItem(info.position) as OnlineFile
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.type = FileUtil.getMimeType(file.getName())
                intent.putExtra(Intent.EXTRA_TITLE, file.getName())
                startActivityForResult(intent, getRequestCode(file, FileAction.DOWNLOAD_SAVE))
                true
            }
            R.id.filestorage_action_open -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val file = file_list?.adapter?.getItem(info.position) as OnlineFile
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
                            check(action.target is OnlineFile) { "Invalid target; must be of type OnlineFile" }
                            val outputStream = contentResolver.openOutputStream(data.data!!, "w") ?: return@withContext
                            val inputStream = URL(action.target.getTempDownloadUrl().url).openStream()
                            val buffer = ByteArray(2048)
                            while (true) {
                                val read = inputStream.read(buffer)
                                if (read <= 0)
                                    break
                                outputStream.write(buffer, 0, read)
                            }
                            outputStream.close()
                            inputStream.close()
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@FilesActivity, R.string.download_finished, Toast.LENGTH_LONG).show()
                        }
                    }
                }
                FileAction.UPLOAD_DOCUMENT -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        withContext(Dispatchers.IO) {
                            try {
                                val inputStream = contentResolver.openInputStream(data.data!!) ?: return@withContext
                                val sessionFile = AuthStore.getAppUser().addSessionFile(queryFileName(data.data!!), byteArrayOf())
                                val buffer = ByteArray(1024 * 1024)
                                while (true) {
                                    val read = inputStream.read(buffer)
                                    if (read < 0) break
                                    if (read != buffer.size) {
                                        val newBuffer = ByteArray(read)
                                        System.arraycopy(buffer, 0, newBuffer, 0, read)
                                        sessionFile.appendData(newBuffer)
                                    } else {
                                        sessionFile.appendData(buffer)
                                    }
                                }
                                val newFile = fileStorage.importSessionFile(sessionFile)
                                val adapter = (file_list.adapter as FileStorageFilesAdapter)
                                sessionFile.delete()
                                withContext(Dispatchers.Main) {
                                    adapter.insert(newFile, 0)
                                    adapter.notifyDataSetChanged()
                                    Toast.makeText(
                                        this@FilesActivity,
                                        R.string.upload_finished,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@FilesActivity, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
                                e.printStackTrace()
                            }
                        }
                    }
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

    private fun openFile(file: OnlineFile) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.IO) {
                val mime = FileUtil.getMimeType(file.getName())
                val download = file.getTempDownloadUrl().url
                val inputStream = URL(download).openStream() ?: return@withContext
                val tempDir = File(cacheDir, "filestorage")
                if (!tempDir.isDirectory)
                    tempDir.mkdir()
                val tempFile = File(tempDir, file.getName())
                inputStream.copyTo(tempFile.outputStream())
                inputStream.close()
                val fileUri = FileProvider.getUriForFile(this@FilesActivity, FILE_PROVIDER_AUTHORITY, tempFile)

                val sendIntent = Intent(Intent.ACTION_SEND)
                sendIntent.type = mime
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, normalizeFileName(file.getName()))
                val viewIntent = Intent(Intent.ACTION_VIEW)
                viewIntent.setDataAndType(fileUri, mime)
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(sendIntent, file.getName()).apply { putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent)) })
            }
        }
    }

    private fun getRequestCode(target: Any?, action: FileAction): Int {
        val id = requestedActivities.size
        requestedActivities[id] = RequestableAction(target, action)
        return id
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