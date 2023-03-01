package de.deftk.openww.android.fragments.feature.filestorage

import android.os.Bundle
import android.text.format.Formatter
import android.view.*
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentEditFileBinding
import de.deftk.openww.android.feature.filestorage.FileCacheElement
import de.deftk.openww.android.filter.FileStorageFileFilter
import de.deftk.openww.android.fragments.ContextualFragment
import de.deftk.openww.android.utils.AndroidUtil
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.FileStorageViewModel
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.filestorage.FileType

class EditFileFragment : ContextualFragment(true) {

    //TODO options menu

    //TODO allow editing notificationLogins (+ also for folders)

    private val fileStorageViewModel: FileStorageViewModel by activityViewModels()
    private val args: EditFileFragmentArgs by navArgs()

    private lateinit var binding: FragmentEditFileBinding
    private lateinit var scope: IOperatingScope
    private lateinit var file: FileCacheElement

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditFileBinding.inflate(inflater, container, false)

        val foundScope = loginViewModel.apiContext.value?.findOperatingScope(args.scope)
        if (foundScope == null) {
            Reporter.reportException(R.string.error_scope_not_found, args.scope, requireContext())
            navController.popBackStack()
            return binding.root
        }
        scope = foundScope

        val filter = FileStorageFileFilter()
        filter.parentCriteria.value = args.parentId
        fileStorageViewModel.fileFilter.value = filter
        fileStorageViewModel.getFilteredFiles(scope).observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                val foundFile = response.value.firstOrNull { it.file.id == args.fileId }
                if (foundFile == null) {
                    Reporter.reportException(R.string.error_file_not_found, args.fileId, requireContext())
                    navController.popBackStack()
                    return@observe
                }
                file = foundFile

                binding.fileName.setText(file.file.name)
                binding.fileCreatedAuthor.text = file.file.created.member.name
                binding.fileCreatedDate.text = getString(R.string.created_date).format(TextUtils.parseShortDate(file.file.created.date))
                binding.fileModifiedAuthor.text = file.file.modified.member.name
                binding.fileModifiedAuthor.isVisible = file.file.created.member != file.file.modified.member
                binding.fileModifiedDate.text = getString(R.string.modified_date).format(TextUtils.parseShortDate(file.file.modified.date))
                binding.fileModifiedDate.isVisible = file.file.created.date != file.file.modified.date

                when (file.file.type) {
                    FileType.FILE -> binding.fileSize.text = getString(R.string.size).format(
                        Formatter.formatFileSize(requireContext(), file.file.size))
                    FileType.FOLDER -> binding.fileSize.text = getString(R.string.children_count).format(fileStorageViewModel.getCachedChildren(scope, file.file.id).size.takeIf { it != 0 }?.toString() ?: getString(R.string.unknown))
                }
                binding.fileIsMine.isChecked = file.file.mine == true
                binding.fileIsShared.isChecked = file.file.shared == true
                binding.fileIsSparse.isChecked = file.file.sparse == true

                binding.filePermissionReadable.isChecked = file.file.readable == true
                binding.filePermissionWritable.isChecked = file.file.writable == true
                binding.filePermissionEffectiveRead.isChecked = file.file.effectiveRead == true
                binding.filePermissionEffectiveCreate.isChecked = file.file.effectiveCreate == true
                binding.filePermissionEffectiveModify.isChecked = file.file.effectiveModify == true
                binding.filePermissionEffectiveDelete.isChecked = file.file.effectiveDelete == true

                binding.fileSelfDownloadNotification.isChecked = file.file.downloadNotification?.me == true
                binding.fileDownloadNotificationListDescription.isVisible = file.file.downloadNotification?.users?.isNotEmpty() == true
                binding.fileDownloadNotificationList.text = file.file.downloadNotification?.users?.joinToString("\n") { it.alias ?: it.name } ?: ""
                binding.fileDescription.setText(file.file.description ?: "")
                setUIState(UIState.READY)
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_get_files_failed, response.exception, requireContext())
                navController.popBackStack()
            }
        }

        loginViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                //TODO implement
            } else {
                binding.fileDescription.setText("")
                binding.fileSelfDownloadNotification.isChecked = false
                binding.filePermissionEffectiveDelete.isChecked = false
                binding.filePermissionEffectiveModify.isChecked = false
                binding.filePermissionEffectiveCreate.isChecked = false
                binding.filePermissionEffectiveRead.isChecked = false
                binding.filePermissionWritable.isChecked = false
                binding.filePermissionReadable.isChecked = false
                binding.fileIsSparse.isChecked = false
                binding.fileIsShared.isChecked = false
                binding.fileIsMine.isChecked = false
                binding.fabEditFile.isEnabled = false
                binding.fileCreatedAuthor.text = ""
                binding.fileDownloadNotificationList.text = ""
                binding.fileDownloadNotificationListDescription.text = ""
                binding.fileModifiedAuthor.text = ""
                binding.fileModifiedDate.text = ""
                binding.fileName.setText("")
                binding.fileCreatedDate.text = ""
                binding.fileSize.text = ""
                setUIState(UIState.DISABLED)
            }
        }

        fileStorageViewModel.editFileResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                fileStorageViewModel.resetEditFileResponse() // mark as handled

            if (response is Response.Success) {
                setUIState(UIState.READY)
                AndroidUtil.hideKeyboard(requireActivity(), requireView())
                navController.popBackStack()
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_save_changes_failed, response.exception, requireContext())
            }
        }

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.edit_options_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.edit_options_item_save) {
            val apiContext = loginViewModel.apiContext.value ?: return false

            if (binding.fileName.text.isBlank()) {
                Toast.makeText(requireContext(), R.string.invalid_file_name, Toast.LENGTH_SHORT).show()
                return true
            }
            when (file.file.type) {
                FileType.FILE -> fileStorageViewModel.editFile(file.file, binding.fileName.text.toString(), binding.fileDescription.text.toString(), binding.fileSelfDownloadNotification.isChecked, scope, apiContext)
                //TODO upload notifications (folders)
                FileType.FOLDER -> fileStorageViewModel.editFolder(file.file, binding.fileName.text.toString(), binding.fileDescription.text.toString(), binding.filePermissionReadable.isChecked, binding.filePermissionWritable.isChecked, null, scope, apiContext)
            }
            setUIState(UIState.LOADING)
        }
        return false
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.fileName.isEnabled = newState == UIState.READY
        binding.fileDescription.isEnabled = newState == UIState.READY
        binding.filePermissionReadable.isEnabled = newState == UIState.READY && file.file.type == FileType.FOLDER
        binding.filePermissionWritable.isEnabled = newState == UIState.READY && file.file.type == FileType.FOLDER
        binding.fileSelfDownloadNotification.isEnabled = newState == UIState.READY && file.file.type == FileType.FILE
    }
}