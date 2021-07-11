package de.deftk.openww.android.fragments.feature.filestorage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ActionModeAdapter
import de.deftk.openww.android.adapter.recycler.FileAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentFilesBinding
import de.deftk.openww.android.feature.AbstractNotifyingWorker
import de.deftk.openww.android.feature.filestorage.DownloadOpenWorker
import de.deftk.openww.android.feature.filestorage.NetworkTransfer
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.utils.FileUtil
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.FileStorageViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.filestorage.FileType
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import java.io.File
import kotlin.math.max

class FilesFragment : ActionModeFragment<IRemoteFile, FileAdapter.FileViewHolder>(R.menu.filestorage_actionmode_menu) {

    //TODO cancel ongoing network transfers on account switch

    private val args: FilesFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val fileStorageViewModel: FileStorageViewModel by activityViewModels()
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }
    private val workManager by lazy { WorkManager.getInstance(requireContext()) }
    private val navController by lazy { findNavController() }

    private lateinit var downloadSaveLauncher: ActivityResultLauncher<Pair<Intent, IRemoteFile>>
    private lateinit var binding: FragmentFilesBinding
    private lateinit var scope: IOperatingScope

    private var currentNetworkTransfers = emptyList<NetworkTransfer>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFilesBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        val argScope = userViewModel.apiContext.value?.findOperatingScope(args.operatorId)
        if (argScope == null) {
            Reporter.reportException(R.string.error_scope_not_found, args.operatorId, requireContext())
            navController.popBackStack(R.id.fileStorageGroupFragment, false)
            return binding.root
        }
        scope = argScope

        binding.fileList.adapter = adapter
        binding.fileList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.fileList.recycledViewPool.setMaxRecycledViews(0, 0) // this is just a workaround (otherwise preview images disappear while scrolling, see https://github.com/square/picasso/issues/845#issuecomment-280626688) FIXME seems like an issue with recycling

        fileStorageViewModel.getProviderLiveData(scope, args.folderId, args.path?.toList()).observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value.map { it.file })
                binding.fileEmpty.isVisible = response.value.isEmpty()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_files_failed, response.exception, requireContext())
            }
            binding.progressFileStorage.isVisible = false
            binding.fileStorageSwipeRefresh.isRefreshing = false
        }

        fileStorageViewModel.networkTransfers.observe(viewLifecycleOwner) { transfers ->
            for (i in 0 until max(transfers.size, currentNetworkTransfers.size)) {
                if (i < transfers.size && !currentNetworkTransfers.contains(transfers[i])) {
                    // handle new transfer
                    val transfer = transfers[i]
                    onNetworkTransferAdded(transfer)
                    continue
                }
                if (i < currentNetworkTransfers.size && !transfers.contains(currentNetworkTransfers[i])) {
                    // handle removed transfer
                    val transfer = currentNetworkTransfers[i]
                    onNetworkTransferRemoved(transfer)
                    continue
                }
            }
            currentNetworkTransfers = transfers
        }

        fileStorageViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                fileStorageViewModel.resetBatchDeleteResponse()

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
                binding.progressFileStorage.isVisible = false
            } else {
                actionMode?.finish()
            }
        }

        binding.fileStorageSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                fileStorageViewModel.cleanCache(scope, args.folderId, args.path?.toList())
                fileStorageViewModel.loadFiles(scope, args.folderId, args.path?.toList(), apiContext)
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                val newScope = userViewModel.apiContext.value?.findOperatingScope(args.operatorId)
                if (newScope == null) {
                    navController.popBackStack(R.id.fileStorageGroupFragment, false)
                    return@observe
                } else {
                    this.scope = newScope
                    fileStorageViewModel.loadFiles(newScope, args.folderId, args.path?.toList(), apiContext)
                }
            } else {
                navController.popBackStack(R.id.fileStorageGroupFragment, false)
            }
        }

        downloadSaveLauncher = registerForActivityResult(SaveFileContract()) { (result, file) ->
            val uri = result.data?.data
            userViewModel.apiContext.value?.also { apiContext ->
                fileStorageViewModel.startSaveDownload(workManager, apiContext, file, scope, uri.toString())
            }
        }

        registerForContextMenu(binding.fileList)
        return binding.root
    }

    override fun createAdapter(): ActionModeAdapter<IRemoteFile, FileAdapter.FileViewHolder> {
        return FileAdapter(scope, this, args.folderId, args.path, fileStorageViewModel)
    }

    private fun onNetworkTransferAdded(transfer: NetworkTransfer) {
        val liveData = workManager.getWorkInfoByIdLiveData(transfer.workerId)
        if (transfer is NetworkTransfer.DownloadOpen) {
            liveData.observe(viewLifecycleOwner) { workInfo ->
                val adapterIndex = adapter.currentList.indexOfFirst { it.id == transfer.id }
                var progress = workInfo.progress.getInt(AbstractNotifyingWorker.ARGUMENT_PROGRESS, 0)
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        progress = 100
                        val fileUri = Uri.parse(workInfo.outputData.getString(DownloadOpenWorker.DATA_FILE_URI))
                        val fileName = workInfo.outputData.getString(DownloadOpenWorker.DATA_FILE_NAME)!!
                        FileUtil.showFileOpenIntent(fileName, fileUri, preferences, requireContext())
                        fileStorageViewModel.hideNetworkTransfer(transfer)
                    }
                    WorkInfo.State.CANCELLED -> {
                        //TODO remove notification
                        progress = -1
                        fileStorageViewModel.hideNetworkTransfer(transfer)
                    }
                    WorkInfo.State.FAILED -> {
                        val message = workInfo.outputData.getString(AbstractNotifyingWorker.DATA_ERROR_MESSAGE) ?: "Unknown"
                        Reporter.reportException(R.string.error_download_worker_failed, message, requireContext())
                        progress = -1
                        fileStorageViewModel.hideNetworkTransfer(transfer)
                    }
                    else -> { /* ignore */ }
                }
                transfer.progress = progress
                val viewHolder = binding.fileList.findViewHolderForAdapterPosition(adapterIndex) as FileAdapter.FileViewHolder
                viewHolder.setProgress(progress)
            }
        } else if (transfer is NetworkTransfer.DownloadSave) {
            liveData.observe(viewLifecycleOwner) { workInfo ->
                val adapterIndex = adapter.currentList.indexOfFirst { it.id == transfer.id }
                var progress = workInfo.progress.getInt(AbstractNotifyingWorker.ARGUMENT_PROGRESS, 0)
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        progress = 100
                        Toast.makeText(requireContext(), R.string.download_finished, Toast.LENGTH_LONG).show()
                        fileStorageViewModel.hideNetworkTransfer(transfer)
                    }
                    WorkInfo.State.CANCELLED -> {
                        //TODO remove notification
                        progress = -1
                        fileStorageViewModel.hideNetworkTransfer(transfer)
                    }
                    WorkInfo.State.FAILED -> {
                        val message = workInfo.outputData.getString(AbstractNotifyingWorker.DATA_ERROR_MESSAGE) ?: "Unknown"
                        Reporter.reportException(R.string.error_download_worker_failed, message, requireContext())
                        progress = -1
                        fileStorageViewModel.hideNetworkTransfer(transfer)
                    }
                    else -> { /* ignore */ }
                }
                transfer.progress = progress
                val viewHolder = binding.fileList.findViewHolderForAdapterPosition(adapterIndex) as FileAdapter.FileViewHolder
                viewHolder.setProgress(progress)
            }
        }
    }

    private fun onNetworkTransferRemoved(transfer: NetworkTransfer) {
        /*if (transfer is NetworkTransfer.Upload) {

        }*/
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        requireActivity().menuInflater.inflate(R.menu.filestorage_list_menu, menu)
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            val file = (binding.fileList.adapter as FileAdapter).getItem(menuInfo.position)
            if (file.effectiveRead == true) {
                requireActivity().menuInflater.inflate(R.menu.filestorage_read_list_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.fileList.adapter as FileAdapter
        return when (item.itemId) {
            R.id.filestorage_action_info -> {
                val file = adapter.getItem(menuInfo.position)
                navController.navigate(FilesFragmentDirections.actionFilesFragmentToReadFileFragment(args.operatorId, file.id, args.folderId, args.path))
                true
            }
            R.id.filestorage_action_download -> {
                val file = adapter.getItem(menuInfo.position)
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.type = FileUtil.getMimeType(file.name)
                intent.putExtra(Intent.EXTRA_TITLE, file.name)
                downloadSaveLauncher.launch(intent to file)
                true
            }
            R.id.filestorage_action_open -> {
                val file = adapter.getItem(menuInfo.position)
                openFile(file)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val canDelete = adapter.selectedItems.all { it.binding.file!!.effectiveDelete == true }
        menu.findItem(R.id.filestorage_action_delete).isEnabled = canDelete //TODO should be visible if disabled
        return super.onPrepareActionMode(mode, menu)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filestorage_action_delete -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    fileStorageViewModel.batchDelete(adapter.selectedItems.map { it.binding.file!! }, args.folderId, args.path?.toList(), scope, apiContext)
                    binding.progressFileStorage.isVisible = true
                }
            }
            else -> return false
        }
        return true
    }

    override fun onItemClick(view: View, viewHolder: FileAdapter.FileViewHolder) {
        if (viewHolder.binding.file!!.type == FileType.FOLDER) {
            val path = if (viewHolder.binding.folderId != null) {
                if (viewHolder.binding.path != null)
                    arrayOf(*viewHolder.binding.path!!, viewHolder.binding.folderId!!)
                else arrayOf(viewHolder.binding.folderId!!)
            } else null
            val action = FilesFragmentDirections.actionFilesFragmentSelf(viewHolder.binding.file!!.id, viewHolder.binding.scope!!.login, viewHolder.binding.file!!.name, path)
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
            userViewModel.apiContext.value?.also { apiContext ->
                fileStorageViewModel.startOpenDownload(workManager, apiContext, file, scope, tempFile.absolutePath)
            }
        }
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