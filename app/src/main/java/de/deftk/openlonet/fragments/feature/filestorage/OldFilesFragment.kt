package de.deftk.openlonet.fragments.feature.filestorage

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.*
import android.widget.AdapterView
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import de.deftk.lonet.api.LoNetClient
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.lonet.api.implementation.feature.filestorage.RemoteFile
import de.deftk.lonet.api.implementation.feature.filestorage.session.SessionFile
import de.deftk.lonet.api.model.IOperatingScope
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.filestorage.FileType
import de.deftk.lonet.api.model.feature.filestorage.IRemoteFile
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.FileStorageFilesAdapter
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.databinding.FragmentFilesBinding
import de.deftk.openlonet.feature.filestorage.DownloadOpenWorker
import de.deftk.openlonet.feature.filestorage.DownloadSaveWorker
import de.deftk.openlonet.feature.filestorage.UploadWorker
import de.deftk.openlonet.fragments.AbstractListFragment
import de.deftk.openlonet.utils.FileUtil
import de.deftk.openlonet.viewmodel.FileStorageViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.io.File

class OldFilesFragment : AbstractListFragment<IRemoteFile>() {

    private val args: FilesFragmentArgs by navArgs()
    private val fileStorageViewModel: FileStorageViewModel by activityViewModels()
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }
    private val workManager by lazy { WorkManager.getInstance(requireContext()) }
    private val requestedActions = mutableMapOf<Int, RequestableAction>()

    override val dataHolder: Lazy<LiveData<List<IRemoteFile>>> = lazy { /*fileStorageViewModel.getFolderDataHolder(operator, args.path, args.folderId)*/ TODO("") }

    private lateinit var binding: FragmentFilesBinding
    private lateinit var operator: IOperatingScope
    private var folder: IRemoteFile? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        operator = userViewModel.apiContext.value?.findOperatingScope(args.operatorId) ?: error("failed to find operator ${args.operatorId}")
        if (args.folderId != null) {
            userViewModel.apiContext.value?.apply {
                //folder = fileStorageViewModel.findFolderLiveData(fileStorageViewModel.getRootFiles(operator).value ?: emptyList(), args.path?.toMutableList(), args.folderId!!)?.file
            }
        }
        super.onViewCreated(view, savedInstanceState)

        val writeAccess = (operator.effectiveRights.contains(Permission.FILES_WRITE) || operator.effectiveRights.contains(
            Permission.FILES_ADMIN)) && (folder == null || folder?.effectiveModify() == true)
        binding.fabUploadFile.isVisible = writeAccess
        if (writeAccess) {
            binding.fabUploadFile.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.type = "*/*"
                startActivityForResult(intent, getRequestCode(null, FileAction.UPLOAD_DOCUMENT))
            }
        }

        registerForContextMenu(binding.fileList)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        if (menuInfo is AdapterView.AdapterContextMenuInfo) {
            /*val file = binding.fileList.adapter?.getItem(menuInfo.position) as? IRemoteFile?
            if (file?.effectiveRead() == true) {
                activity?.menuInflater?.inflate(R.menu.filestorage_read_list_menu, menu)
            }*/
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filestorage_action_download -> {
                /*val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val file = binding.fileList.adapter?.getItem(info.position) as? IRemoteFile? ?: return false
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.type = FileUtil.getMimeType(file.name)
                intent.putExtra(Intent.EXTRA_TITLE, file.name)
                startActivityForResult(intent, getRequestCode(file, FileAction.DOWNLOAD_SAVE))*/
                true
            }
            R.id.filestorage_action_open -> {
                /*val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val file = binding.fileList.adapter?.getItem(info.position) as? IRemoteFile? ?: return false
                openFile(file)*/
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val action = requestedActions[requestCode]
        if (action != null && resultCode == Activity.RESULT_OK && data?.data != null) {
            when (action.action) {
                FileAction.DOWNLOAD_SAVE -> {
                    val apiContext = userViewModel.apiContext.value
                    if (apiContext != null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            check(action.target is RemoteFile) { "Invalid target; must be of type OnlineFile" }
                            val downloadUrl = withContext(Dispatchers.IO) {
                                action.target.getDownloadUrl(operator.getRequestContext(apiContext)).url
                            }
                            doSaveDownload(data.data!!.toString(), downloadUrl, action.target)
                        }
                    }
                }
                FileAction.UPLOAD_DOCUMENT -> {
                    val uri = data.data!!
                    doUpload(uri, getFileName(uri))
                }
            }
        }
    }

    override fun startRefreshDataHolder(apiContext: ApiContext) {
        /*fileStorageViewModel.invalidateFilePreviews(operator)
        userViewModel.apiContext.value?.apply {
            if (args.folderId != null) {
                fileStorageViewModel.cacheDirectory(operator, args.path?.toMutableList(), args.folderId!!, this).observe(viewLifecycleOwner) { result ->
                    if (result is Response.Failure) {
                        //TODO handle error
                        result.exception.printStackTrace()
                    }
                }
            } else {
                fileStorageViewModel.refreshRootFiles(operator, apiContext).observe(viewLifecycleOwner) { result ->
                    if (result is Response.Failure) {
                        //TODO handle error
                        result.exception.printStackTrace()
                    }
                }
            }
        }*/
    }

    override fun getListView(): ListView {
        TODO("")
        //return binding.fileList
    }

    override fun getSwipeRefreshLayout(): SwipeRefreshLayout {
        return binding.fileStorageSwipeRefresh
    }

    override fun createAdapter(elements: List<IRemoteFile>): ListAdapter {
        return FileStorageFilesAdapter(requireContext(), elements, operator, userViewModel, fileStorageViewModel, viewLifecycleOwner)
    }

    override fun disableLoading(emptyResult: Boolean) {
        binding.fileEmpty.isVisible = emptyResult
        binding.progressFileStorage.visibility = View.GONE
    }

    override fun showDetails(item: IRemoteFile, view: View) {
        if (item.type == FileType.FILE) {
            if (item.effectiveRead() == true) {
                openFile(item)
            }
        } else if (item.type == FileType.FOLDER) {
            val path = if (folder != null) {
                if (args.path != null)
                    arrayOf(*args.path!!, folder!!.id)
                else arrayOf(folder!!.id)
            } else null
            val action = FilesFragmentDirections.actionFilesFragmentSelf(item.id, operator.login, item.name, path)
            navController.navigate(action)
        }
    }

    private fun openFile(file: IRemoteFile) {
        val apiContext = userViewModel.apiContext.value
        if (apiContext != null) {
            CoroutineScope(Dispatchers.Main).launch {
                val downloadUrl = withContext(Dispatchers.IO) {
                    file.getDownloadUrl(operator.getRequestContext(apiContext)).url
                }
                val tempDir = File(requireActivity().cacheDir, "filestorage")
                if (!tempDir.exists())
                    tempDir.mkdir()
                val tempFile = File(tempDir, file.name)
                doOpenDownload(tempFile.absolutePath, downloadUrl, file)
            }
        }
    }

    private fun doOpenDownload(destinationUrl: String, downloadUrl: String, file: IRemoteFile) {
        val workRequest = OneTimeWorkRequestBuilder<DownloadOpenWorker>()
            .setInputData(
                workDataOf(
                DownloadOpenWorker.DATA_DESTINATION_URI to destinationUrl,
                DownloadOpenWorker.DATA_DOWNLOAD_URL to downloadUrl,
                DownloadOpenWorker.DATA_FILE_NAME to file.name,
                DownloadOpenWorker.DATA_FILE_SIZE to file.getSize()
            )
            )
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
                    startActivity(Intent.createChooser(sendIntent, file.name).apply { putExtra(
                        Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent)) })
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

    private fun doSaveDownload(destinationUrl: String, downloadUrl: String, file: IRemoteFile) {
        val workRequest = OneTimeWorkRequestBuilder<DownloadSaveWorker>()
            .setInputData(
                workDataOf(
                DownloadSaveWorker.DATA_DOWNLOAD_URL to downloadUrl,
                DownloadSaveWorker.DATA_DESTINATION_URI to destinationUrl,
                DownloadSaveWorker.DATA_FILE_NAME to file.name,
                DownloadSaveWorker.DATA_FILE_SIZE to file.getSize()
            )
            )
            .build()
        workManager.getWorkInfoByIdLiveData(workRequest.id).observe(this) { workInfo ->
            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> {
                    Toast.makeText(requireContext(), R.string.download_finished, Toast.LENGTH_LONG).show()
                }
                WorkInfo.State.CANCELLED -> {
                    //TODO delete local file
                }
                WorkInfo.State.FAILED -> {
                    //TODO handle error
                }
                else -> { /* ignore */ }
            }
        }
        workManager.enqueue(workRequest)
    }

    private fun doUpload(uri: Uri, fileName: String) {
        val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(
                workDataOf(
                UploadWorker.DATA_FILE_URI to uri.toString(),
                UploadWorker.DATA_FILE_NAME to fileName
            )
            )
            .build()
        workManager.getWorkInfoByIdLiveData(workRequest.id).observe(this) { workInfo ->
            if (workInfo != null) {
                //val progress = workInfo.progress.getInt(UploadWorker.ARGUMENT_PROGRESS, 0)

                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val apiContext = userViewModel.apiContext.value ?: return@observe
                        val sessionFile = LoNetClient.json.decodeFromString<SessionFile>(workInfo.outputData.getString(
                            UploadWorker.DATA_SESSION_FILE)!!)
                        CoroutineScope(Dispatchers.IO).launch {
                            val newFile = operator.importSessionFile(sessionFile, context = operator.getRequestContext(apiContext))
                            sessionFile.delete(apiContext.getUser().getRequestContext(apiContext))

                            withContext(Dispatchers.Main) {
                                //TODO don't do this; rather insert into viewModel
                                val adapter = (binding.fileList.adapter as FileStorageFilesAdapter)
                                adapter.insert(newFile, 0)
                                adapter.notifyDataSetChanged()
                                Toast.makeText(
                                    requireContext(),
                                    R.string.upload_finished,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    WorkInfo.State.CANCELLED -> {
                        //TODO delete session file
                    }
                    WorkInfo.State.FAILED -> {
                        //TODO handle error
                    }
                    else -> { /* ignore */ }
                }
            }
        }
        workManager.enqueue(workRequest)
    }

    private fun getRequestCode(target: Any?, action: FileAction): Int {
        val id = requestedActions.size
        requestedActions[id] = RequestableAction(target, action)
        return id
    }

    private fun getFileName(uri: Uri): String {
        var cursor: Cursor? = null
        var filename = "unknown.bin"
        try {
            cursor = requireActivity().contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)

            if (cursor?.moveToFirst() == true) {
                filename = cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
            }
        } finally {
            cursor?.close()
        }
        return filename
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