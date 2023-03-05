package de.deftk.openww.android.fragments.feature.filestorage

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import androidx.work.WorkManager
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ActionModeAdapter
import de.deftk.openww.android.adapter.recycler.FileAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentFilesBinding
import de.deftk.openww.android.feature.AbstractNotifyingWorker
import de.deftk.openww.android.feature.LaunchMode
import de.deftk.openww.android.feature.filestorage.*
import de.deftk.openww.android.filter.FileStorageFileFilter
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.utils.FileUtil
import de.deftk.openww.android.utils.ISearchProvider
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.FileStorageViewModel
import de.deftk.openww.api.WebWeaverClient
import de.deftk.openww.api.implementation.feature.filestorage.session.SessionFile
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.filestorage.FileType
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import kotlinx.serialization.decodeFromString
import java.io.File
import kotlin.math.max

class FilesFragment : ActionModeFragment<IRemoteFile, FileAdapter.FileViewHolder>(R.menu.filestorage_actionmode_menu), ISearchProvider {

    //TODO needs recode to remove title from navargs and being able to be called by deeplink

    //TODO cancel ongoing network transfers on account switch

    private val args: FilesFragmentArgs by navArgs()
    private val fileStorageViewModel: FileStorageViewModel by activityViewModels()
    private val workManager by lazy { WorkManager.getInstance(requireContext()) }

    private lateinit var downloadSaveLauncher: ActivityResultLauncher<Pair<Intent, IRemoteFile>>
    private lateinit var uploadLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var binding: FragmentFilesBinding
    private lateinit var searchView: SearchView

    private var scope: IOperatingScope? = null
    private var folderId: String? = null
    private var cachedNetworkTransfers = emptyList<NetworkTransfer>()
    private var animationShown = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFilesBinding.inflate(inflater, container, false)

        fileStorageViewModel.networkTransfers.observe(viewLifecycleOwner) { transfers ->
            for (i in 0 until max(transfers.size, cachedNetworkTransfers.size)) {
                if (i < transfers.size && !cachedNetworkTransfers.contains(transfers[i])) {
                    // handle new transfer
                    val transfer = transfers[i]
                    onNetworkTransferAdded(transfer)
                    continue
                }
                if (i < cachedNetworkTransfers.size && !transfers.contains(cachedNetworkTransfers[i])) {
                    // handle removed transfer
                    val transfer = cachedNetworkTransfers[i]
                    onNetworkTransferRemoved(transfer)
                    continue
                }
            }
            cachedNetworkTransfers = transfers
        }

        fileStorageViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                fileStorageViewModel.resetBatchDeleteResponse()

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
            } else {
                setUIState(UIState.READY)
                actionMode?.finish()
            }
        }

        fileStorageViewModel.importSessionFile.observe(viewLifecycleOwner) { data ->
            if (data != null)
                fileStorageViewModel.resetImportSessionFileResponse()

            val response = data?.first
            if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_import_session_file_failed, response.exception, requireContext())
            } else if (response is Response.Success) {
                setUIState(UIState.READY)
                val receiveDownloadNotification = data.second
                if (receiveDownloadNotification) {
                    loginViewModel.apiContext.value?.apply {
                        val file = response.value
                        fileStorageViewModel.editFile(file, file.name, file.description, true, scope!!, this)
                    }
                    return@observe
                }
            }
        }

        fileStorageViewModel.editFileResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                fileStorageViewModel.resetEditFileResponse()

            if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_edit_file_failed, response.exception, requireContext())
            } else if (response is Response.Success) {
                setUIState(UIState.READY)
            }
        }

        fileStorageViewModel.addFolderResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                fileStorageViewModel.resetAddFolderResponse()

            if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_add_folder_failed, response.exception, requireContext())
            } else if (response is Response.Success) {
                setUIState(UIState.READY)
            }
        }

        binding.fileStorageSwipeRefresh.setOnRefreshListener {
            loginViewModel.apiContext.value?.also { apiContext ->
                fileStorageViewModel.cleanCache(scope!!)
                fileStorageViewModel.loadChildrenTree(scope!!, folderId!!, true, apiContext)
                adapter.notifyDataSetChanged() // update previews
            }
        }

        binding.fileList.layoutManager = object : LinearLayoutManager(requireContext()) {
            override fun onLayoutCompleted(state: RecyclerView.State?) {
                super.onLayoutCompleted(state)
                val items = adapter.currentList
                if (!animationShown &&  (args.highlightFileId != null || args.highlightFileName != null) && items.isNotEmpty()) {
                    val file = items.firstOrNull { it.name == args.highlightFileName || it.id == args.highlightFileId }
                    if (file != null) {
                        val position = adapter.currentList.indexOf(file)
                        if (position != -1) {
                            binding.fileList.highlightItem(position)
                            animationShown = true
                        }
                    }
                }
            }
        }

        loginViewModel.apiContext.observe(viewLifecycleOwner) apiContext@ { apiContext ->
            if (apiContext != null) {
                val newScope = loginViewModel.apiContext.value?.findOperatingScope(args.operatorId)
                if (newScope == null) {
                    setUIState(UIState.DISABLED)
                    Reporter.reportException(R.string.error_scope_not_found, args.operatorId, requireContext())
                    navController.popBackStack(R.id.fileStorageGroupFragment, true)
                    return@apiContext
                }
                if (!Feature.FILES.isAvailable(newScope.effectiveRights)) {
                    setUIState(UIState.DISABLED)
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack(R.id.fileStorageGroupFragment, true)
                    return@apiContext
                }

                if (scope != null) {
                    fileStorageViewModel.getFilteredFiles(scope!!).removeObservers(viewLifecycleOwner)
                    scope = newScope
                    (adapter as FileAdapter).scope = newScope
                } else {
                    scope = newScope
                }
                binding.fileList.adapter = adapter
                binding.fileList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
                binding.fileList.recycledViewPool.setMaxRecycledViews(0, 0) // this is just a workaround (otherwise preview images disappear while scrolling, see https://github.com/square/picasso/issues/845#issuecomment-280626688) FIXME seems like an issue with recycling

                val filter = FileStorageFileFilter()
                if (args.folderNameId == null) {
                    folderId = args.folderId
                    filter.parentCriteria.value = folderId
                    fileStorageViewModel.fileFilter.value = filter
                }
                fileStorageViewModel.getFilteredFiles(scope!!).observe(viewLifecycleOwner) filtered@ { response ->
                    if (response is Response.Success) {
                        setUIState(UIState.READY)
                        if (args.folderNameId != null && folderId == null) {
                            folderId = fileStorageViewModel.resolveNameTree(scope!!, args.folderNameId!!)
                            if (folderId != null) {
                                filter.parentCriteria.value = folderId
                                fileStorageViewModel.fileFilter.value = filter
                                return@filtered
                            }
                        }
                        adapter.submitList(response.value.map { it.file })
                        binding.fileEmpty.isVisible = response.value.isEmpty()
                        updateUploadFab()
                        requireActivity().invalidateOptionsMenu()

                        val parent = fileStorageViewModel.getAllFiles(scope!!).value?.valueOrNull()?.singleOrNull { it.file.id == folderId }
                        if (parent != null) {
                            setTitle(parent.file.name)
                        } else if (folderId == "/") {
                            setTitle(newScope.name)
                        }
                    } else if (response is Response.Failure) {
                        setUIState(UIState.ERROR)
                        Reporter.reportException(R.string.error_get_files_failed, response.exception, requireContext())
                    }
                }

                if (fileStorageViewModel.getAllFiles(scope!!).value == null) {
                    if (args.folderNameId != null) {
                        fileStorageViewModel.loadChildrenNameTree(scope!!, args.folderNameId!!, false, apiContext)
                    } else {
                        if (fileStorageViewModel.getAllFiles(scope!!).value?.valueOrNull()?.any { it.file.parentId == folderId } == true) {
                            fileStorageViewModel.loadChildren(scope!!, folderId!!, false, apiContext)
                        } else {
                            fileStorageViewModel.loadChildrenTree(scope!!, folderId!!, false, apiContext)
                        }
                    }
                    updateUploadFab()
                    setUIState(UIState.LOADING)
                }
            } else {
                binding.fabUploadFile.isVisible = false
                binding.fileEmpty.isVisible = false
                binding.fileStorageSwipeRefresh.isRefreshing = false
                if (scope != null)
                    adapter.submitList(emptyList())
            }
        }

        downloadSaveLauncher = registerForActivityResult(SaveFileContract()) { (result, file) ->
            val uri = result.data?.data
            loginViewModel.apiContext.value?.also { apiContext ->
                fileStorageViewModel.startSaveDownload(workManager, apiContext, file, scope!!, uri.toString())
            }
        }

        uploadLauncher = registerForActivityResult(OpenDocumentsContract()) { uris ->
            uris?.forEach { uri ->
                uploadFile(uri)
            }
        }

        binding.fabUploadFile.setOnClickListener {
            if (args.pasteMode) {
                //TODO don't rely on intent (functionality should be reused for internal copy-paste/cut operations)
                val intent = requireActivity().intent
                //TODO fix deprecation as in LaunchFragment
                if (intent.action == Intent.ACTION_SEND) {
                    uploadFile(requireActivity().intent.getParcelableExtra(Intent.EXTRA_STREAM)!!)
                } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
                    requireActivity().intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)!!.forEach { uri ->
                        uploadFile(uri)
                    }
                }

                binding.fabUploadFile.isVisible = false
            } else {
                uploadLauncher.launch(arrayOf("*/*"))
            }
        }

        registerForContextMenu(binding.fileList)
        return binding.root
    }

    override fun onResume() {
        //FIXME scenario: start download, exit app, download finishes in background, reentering app
        // problem: download/upload indicator is not updated
        // -> update indicator for each transfer here
        super.onResume()
    }

    private fun updateUploadFab() {
        binding.fabUploadFile.isVisible = getProviderFile()?.file?.effectiveCreate == true
        if (args.pasteMode) {
            binding.fabUploadFile.setImageResource(R.drawable.ic_content_paste_24)
        } else {
            binding.fabUploadFile.setImageResource(R.drawable.ic_add_24)
        }
    }

    private fun getProviderFile(): FileCacheElement? {
        if (scope == null)
            return null
        return fileStorageViewModel.getAllFiles(scope!!).value?.valueOrNull()?.firstOrNull { it.file.id == folderId || (it.file.id == "" && folderId == "/") }
    }

    override fun createAdapter(): ActionModeAdapter<IRemoteFile, FileAdapter.FileViewHolder> {
        return FileAdapter(scope!!, this, fileStorageViewModel)
    }

    private fun onNetworkTransferAdded(transfer: NetworkTransfer) {
        val liveData = workManager.getWorkInfoByIdLiveData(transfer.workerId)
        when (transfer) {
            is NetworkTransfer.DownloadOpen -> {
                liveData.observe(viewLifecycleOwner) { workInfo ->
                    val adapterIndex = adapter.currentList.indexOfFirst { it.id == transfer.id }
                    var progressValue = workInfo.progress.getInt(AbstractNotifyingWorker.ARGUMENT_PROGRESS_VALUE, 0)
                    val maxProgress = workInfo.progress.getInt(AbstractNotifyingWorker.ARGUMENT_PROGRESS_MAX, 1)
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            progressValue = maxProgress
                            val fileUri = Uri.parse(workInfo.outputData.getString(DownloadOpenWorker.DATA_FILE_URI))
                            val fileName = workInfo.outputData.getString(DownloadOpenWorker.DATA_FILE_NAME)!!
                            FileUtil.showFileOpenIntent(fileName, fileUri, requireContext())
                            fileStorageViewModel.hideNetworkTransfer(transfer, scope!!)
                        }
                        WorkInfo.State.CANCELLED -> {
                            //TODO remove notification
                            progressValue = 0
                            fileStorageViewModel.hideNetworkTransfer(transfer, scope!!)
                        }
                        WorkInfo.State.FAILED -> {
                            val message = workInfo.outputData.getString(AbstractNotifyingWorker.DATA_ERROR_MESSAGE) ?: "Unknown"
                            Reporter.reportException(R.string.error_download_worker_failed, message, requireContext())
                            progressValue = 0
                            fileStorageViewModel.hideNetworkTransfer(transfer, scope!!)
                        }
                        else -> { /* ignore */ }
                    }
                    transfer.progressValue = progressValue
                    transfer.maxProgress = maxProgress
                    val viewHolder = binding.fileList.findViewHolderForAdapterPosition(adapterIndex) as FileAdapter.FileViewHolder
                    viewHolder.setProgress(progressValue, maxProgress, requireContext())
                }
            }
            is NetworkTransfer.DownloadSave -> {
                liveData.observe(viewLifecycleOwner) { workInfo ->
                    val adapterIndex = adapter.currentList.indexOfFirst { it.id == transfer.id }
                    var progressValue = workInfo.progress.getInt(AbstractNotifyingWorker.ARGUMENT_PROGRESS_VALUE, 0)
                    val maxProgress = workInfo.progress.getInt(AbstractNotifyingWorker.ARGUMENT_PROGRESS_MAX, 1)
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            progressValue = maxProgress
                            fileStorageViewModel.hideNetworkTransfer(transfer, scope!!)
                        }
                        WorkInfo.State.CANCELLED -> {
                            //TODO remove notification
                            progressValue = 0
                            fileStorageViewModel.hideNetworkTransfer(transfer, scope!!)
                        }
                        WorkInfo.State.FAILED -> {
                            val message = workInfo.outputData.getString(AbstractNotifyingWorker.DATA_ERROR_MESSAGE) ?: "Unknown"
                            Reporter.reportException(R.string.error_download_worker_failed, message, requireContext())
                            progressValue = 0
                            fileStorageViewModel.hideNetworkTransfer(transfer, scope!!)
                        }
                        else -> { /* ignore */ }
                    }
                    transfer.progressValue = progressValue
                    transfer.maxProgress = maxProgress
                    val viewHolder = binding.fileList.findViewHolderForAdapterPosition(adapterIndex) as FileAdapter.FileViewHolder
                    viewHolder.setProgress(progressValue, maxProgress, requireContext())
                }
            }
            is NetworkTransfer.Upload -> {
                liveData.observe(viewLifecycleOwner) { workInfo ->
                    val adapterIndex = adapter.currentList.indexOfFirst { it.id == transfer.id }
                    var progressValue = workInfo.progress.getInt(AbstractNotifyingWorker.ARGUMENT_PROGRESS_VALUE, 0)
                    val maxProgress = workInfo.progress.getInt(AbstractNotifyingWorker.ARGUMENT_PROGRESS_MAX, 1)
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            progressValue = maxProgress
                            Toast.makeText(requireContext(), R.string.upload_finished, Toast.LENGTH_LONG).show()
                            fileStorageViewModel.hideNetworkTransfer(transfer, scope!!)
                            val sessionFile = WebWeaverClient.json.decodeFromString<SessionFile>(workInfo.outputData.getString(SessionFileUploadWorker.DATA_SESSION_FILE) ?: "")
                            loginViewModel.apiContext.value?.also { apiContext ->
                                fileStorageViewModel.importSessionFile(sessionFile, getProviderFile()?.file ?: scope!!, transfer.receiveDownloadNotification, scope!!, apiContext)
                            }
                        }
                        WorkInfo.State.CANCELLED -> {
                            //TODO remove notification
                            progressValue = 0
                            fileStorageViewModel.hideNetworkTransfer(transfer, scope!!)
                        }
                        WorkInfo.State.FAILED -> {
                            val message = workInfo.outputData.getString(AbstractNotifyingWorker.DATA_ERROR_MESSAGE) ?: "Unknown"
                            Reporter.reportException(R.string.error_download_worker_failed, message, requireContext())
                            progressValue = 0
                            fileStorageViewModel.hideNetworkTransfer(transfer, scope!!)
                        }
                        else -> { /* ignore */ }
                    }
                    transfer.progressValue = progressValue
                    transfer.maxProgress = maxProgress
                    val viewHolder = binding.fileList.findViewHolderForAdapterPosition(adapterIndex) as? FileAdapter.FileViewHolder?
                    viewHolder?.setProgress(progressValue, maxProgress, requireContext())
                }
            }
        }
    }

    private fun onNetworkTransferRemoved(transfer: NetworkTransfer) {
        if (LaunchMode.getLaunchMode(requireActivity().intent) == LaunchMode.FILE_UPLOAD && fileStorageViewModel.networkTransfers.value?.isEmpty() != false && transfer is NetworkTransfer.Upload) {
            requireActivity().finish()
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.filestorage_options_menu, menu)
        menuInflater.inflate(R.menu.list_options_menu, menu)
        val searchItem = menu.findItem(R.id.list_options_item_search)
        searchView = searchItem.actionView as SearchView
        searchView.setQuery(fileStorageViewModel.fileFilter.value?.smartSearchCriteria?.value, false) // restore recent search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filter = FileStorageFileFilter()
                filter.smartSearchCriteria.value = newText
                filter.parentCriteria.value = folderId
                fileStorageViewModel.fileFilter.value = filter
                return true
            }
        })
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.filestorage_option_item_create_folder -> {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle(R.string.create_new_folder)

                val input = EditText(requireContext())
                input.hint = getString(R.string.name)
                input.inputType = InputType.TYPE_CLASS_TEXT
                builder.setView(input)

                builder.setPositiveButton(R.string.confirm) { _, _ ->
                    loginViewModel.apiContext.value?.apply {
                        fileStorageViewModel.addFolder(input.text.toString(), getProviderFile()!!.file, scope!!, this)
                        setUIState(UIState.LOADING)
                    }
                }
                builder.setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                builder.create().show()
            }
            else -> return false
        }
        return true
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.filestorage_option_item_create_folder)?.isVisible = getProviderFile()?.file?.effectiveCreate == true
    }

    override fun onSearchBackPressed(): Boolean {
        return if (searchView.isIconified) {
            false
        } else {
            searchView.isIconified = true
            searchView.setQuery(null, true)
            true
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            val file = (binding.fileList.adapter as FileAdapter).getItem(menuInfo.position)
            if (file is RemoteFilePlaceholder)
                return
            requireActivity().menuInflater.inflate(R.menu.filestorage_context_menu, menu)
            val canRead = file.effectiveRead == true && file.type == FileType.FILE
            menu.findItem(R.id.filestorage_context_item_open).isVisible = canRead
            menu.findItem(R.id.filestorage_context_item_download).isVisible = canRead
            menu.findItem(R.id.filestorage_context_item_delete).isVisible = file.effectiveDelete == true
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.fileList.adapter as FileAdapter
        return when (item.itemId) {
            R.id.filestorage_context_item_delete -> {
                val file = adapter.getItem(menuInfo.position)
                loginViewModel.apiContext.value?.also { apiContext ->
                    fileStorageViewModel.batchDelete(listOf(file), scope!!, apiContext)
                }
                true
            }
            R.id.filestorage_context_item_info -> {
                val file = adapter.getItem(menuInfo.position)
                navController.navigate(FilesFragmentDirections.actionFilesFragmentToReadFileFragment(args.operatorId, file.id, folderId!!))
                true
            }
            R.id.filestorage_context_item_download -> {
                val file = adapter.getItem(menuInfo.position)
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.type = FileUtil.getMimeType(file.name)
                intent.putExtra(Intent.EXTRA_TITLE, file.name)
                downloadSaveLauncher.launch(intent to file)
                true
            }
            R.id.filestorage_context_item_open -> {
                val file = adapter.getItem(menuInfo.position)
                openFile(file)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val canDelete = adapter.selectedItems.all { it.binding.file!!.effectiveDelete == true }
        menu.findItem(R.id.filestorage_action_item_delete).isEnabled = canDelete
        return super.onPrepareActionMode(mode, menu)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filestorage_action_item_delete -> {
                loginViewModel.apiContext.value?.also { apiContext ->
                    fileStorageViewModel.batchDelete(adapter.selectedItems.map { it.binding.file!! }, scope!!, apiContext)
                    setUIState(UIState.LOADING)
                }
            }
            else -> return false
        }
        return true
    }

    override fun onItemClick(view: View, viewHolder: FileAdapter.FileViewHolder) {
        if (viewHolder.binding.file!!.type == FileType.FOLDER) {
            val action = FilesFragmentDirections.actionFilesFragmentSelf(viewHolder.binding.file!!.id, viewHolder.binding.scope!!.login, pasteMode = args.pasteMode, folderNameId = null)
            navController.navigate(action)
        } else if (viewHolder.binding.file!!.type == FileType.FILE) {
            openFile(viewHolder.binding.file!!)
        }
    }

    private fun openFile(file: IRemoteFile) {
        if (file.type == FileType.FILE) {
            val tempDir = File(requireActivity().cacheDir, "filestorage")
            if (!tempDir.exists())
                tempDir.mkdir()
            val tempFile = File(tempDir, FileUtil.escapeFileName(file.name))
            loginViewModel.apiContext.value?.also { apiContext ->
                fileStorageViewModel.startOpenDownload(workManager, apiContext, file, scope!!, tempFile.absolutePath)
            }
        }
    }

    private fun uploadFile(uri: Uri) {
        val view = layoutInflater.inflate(R.layout.dialog_upload_file, null, false)
        view.findViewById<EditText>(R.id.file_name).setText(FileUtil.uriToFileName(uri, requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle(R.string.upload)
            .setPositiveButton(R.string.upload) { _, _ ->
                loginViewModel.apiContext.value?.also { apiContext ->
                    val name = view.findViewById<EditText>(R.id.file_name).text.toString()
                    val downloadNotification = view.findViewById<CheckBox>(R.id.file_download_notification_me).isChecked
                    fileStorageViewModel.startUpload(workManager, scope!!, apiContext, uri, name, 0, folderId!!, downloadNotification)
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .create()
        dialog.show()
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.fileStorageSwipeRefresh.isEnabled = newState.swipeRefreshEnabled
        binding.fileStorageSwipeRefresh.isRefreshing = newState.refreshing
        binding.fileList.isEnabled = newState.listEnabled
        binding.fileEmpty.isVisible = newState.showEmptyIndicator
        binding.fabUploadFile.isEnabled = newState == UIState.READY
    }
}

class SaveFileContract : ActivityResultContract<Pair<Intent, IRemoteFile>, Pair<ActivityResult, IRemoteFile>>() {

    private lateinit var file: IRemoteFile

    override fun createIntent(context: Context, input: Pair<Intent, IRemoteFile>): Intent {
        file = input.second
        return input.first
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Pair<ActivityResult, IRemoteFile> {
        return Pair(ActivityResult(resultCode, intent), file)
    }
}

class OpenDocumentsContract : ActivityResultContract<Array<String>, Array<Uri>?>() {

    override fun createIntent(context: Context, input: Array<String>): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT)
            .putExtra(Intent.EXTRA_MIME_TYPES, input)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            .setType("*/*")
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Array<Uri>? {
        if (intent == null || resultCode != Activity.RESULT_OK)
            return null

        return if (intent.clipData != null) {
            Array(intent.clipData!!.itemCount) { index -> intent.clipData!!.getItemAt(index).uri }
        } else {
            arrayOf(intent.data!!)
        }
    }
}