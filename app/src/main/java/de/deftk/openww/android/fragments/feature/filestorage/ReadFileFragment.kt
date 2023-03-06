package de.deftk.openww.android.fragments.feature.filestorage

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentReadFileBinding
import de.deftk.openww.android.feature.filestorage.FileCacheElement
import de.deftk.openww.android.filter.FileStorageFileFilter
import de.deftk.openww.android.fragments.ContextualFragment
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.FileStorageViewModel
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.filestorage.FileType

class ReadFileFragment : ContextualFragment(true) {

    //TODO needs recode to remove title from navargs and being able to be called by deeplink

    //TODO options menu

    private val fileStorageViewModel: FileStorageViewModel by activityViewModels()
    private val args: ReadFileFragmentArgs by navArgs()

    private lateinit var binding: FragmentReadFileBinding
    private var scope: IOperatingScope? = null
    private var file: FileCacheElement? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadFileBinding.inflate(inflater, container, false)
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
        fileStorageViewModel.getFilteredFiles(foundScope).observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                val foundFile = response.value.firstOrNull { it.file.id == args.fileId }
                if (foundFile == null) {
                    Reporter.reportException(R.string.error_file_not_found, args.fileId, requireContext())
                    navController.popBackStack()
                    return@observe
                }
                file = foundFile

                binding.fileName.text = foundFile.file.name
                binding.fileCreatedAuthor.text = foundFile.file.created.member.name
                binding.fileCreatedDate.text = getString(R.string.created_date).format(TextUtils.parseShortDate(foundFile.file.created.date))
                binding.fileModifiedAuthor.text = foundFile.file.modified.member.name
                binding.fileModifiedAuthor.isVisible = foundFile.file.created.member != foundFile.file.modified.member
                binding.fileModifiedDate.text = getString(R.string.modified_date).format(TextUtils.parseShortDate(foundFile.file.modified.date))
                binding.fileModifiedDate.isVisible = foundFile.file.created.date != foundFile.file.modified.date

                when (foundFile.file.type) {
                    FileType.FILE -> binding.fileSize.text = getString(R.string.size).format(Formatter.formatFileSize(requireContext(), foundFile.file.size))
                    FileType.FOLDER -> binding.fileSize.text = getString(R.string.children_count).format(fileStorageViewModel.getCachedChildren(foundScope, foundFile.file.id).size.takeIf { it != 0 }?.toString() ?: getString(R.string.unknown))
                }
                binding.fileIsMine.isChecked = foundFile.file.mine == true
                binding.fileIsShared.isChecked = foundFile.file.shared == true
                binding.fileIsSparse.isChecked = foundFile.file.sparse == true

                binding.filePermissionReadable.isChecked = foundFile.file.readable == true
                binding.filePermissionWritable.isChecked = foundFile.file.writable == true
                binding.filePermissionEffectiveRead.isChecked = foundFile.file.effectiveRead == true
                binding.filePermissionEffectiveCreate.isChecked = foundFile.file.effectiveCreate == true
                binding.filePermissionEffectiveModify.isChecked = foundFile.file.effectiveModify == true
                binding.filePermissionEffectiveDelete.isChecked = foundFile.file.effectiveDelete == true

                binding.fileSelfDownloadNotification.isChecked = foundFile.file.downloadNotification?.me == true
                binding.fileDownloadNotificationListDescription.isVisible = foundFile.file.downloadNotification?.users?.isNotEmpty() == true
                binding.fileDownloadNotificationList.text = foundFile.file.downloadNotification?.users?.joinToString("\n") { it.alias ?: it.name } ?: ""
                binding.fileDescription.text = foundFile.file.description ?: ""

                binding.fabEditFile.isVisible = foundFile.file.effectiveModify == true
                invalidateOptionsMenu()
                setUIState(UIState.READY)
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_get_files_failed, response.exception, requireContext())
                navController.popBackStack()
            }
        }

        binding.fabEditFile.setOnClickListener {
            if (file != null && scope != null) {
                navController.navigate(ReadFileFragmentDirections.actionReadFileFragmentToEditFileFragment(file!!.file.name, scope!!.login, file!!.file.id, file!!.file.parentId ?: "/"))
            }
        }

        loginViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (file != null)
                    binding.fabEditFile.isVisible = file!!.file.effectiveModify == true
            } else {
                setUIState(UIState.DISABLED)
                navController.popBackStack()
            }
        }

        return binding.root
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.fabEditFile.isEnabled = newState == UIState.READY
    }
}