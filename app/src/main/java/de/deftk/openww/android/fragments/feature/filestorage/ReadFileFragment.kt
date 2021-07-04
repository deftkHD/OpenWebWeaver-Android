package de.deftk.openww.android.fragments.feature.filestorage

import android.os.Bundle
import android.text.format.Formatter
import android.view.*
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.MemberAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentReadFileBinding
import de.deftk.openww.android.feature.filestorage.FileCacheElement
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.FileStorageViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.filestorage.FileType
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import java.util.*

class ReadFileFragment : Fragment() {

    //TODO options menu

    private val userViewModel: UserViewModel by activityViewModels()
    private val fileStorageViewModel: FileStorageViewModel by activityViewModels()
    private val args: ReadFileFragmentArgs by navArgs()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentReadFileBinding
    private lateinit var scope: IOperatingScope
    private lateinit var file: FileCacheElement

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadFileBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        val foundScope = userViewModel.apiContext.value?.findOperatingScope(args.scope)
        if (foundScope == null) {
            Reporter.reportException(R.string.error_scope_not_found, args.scope, requireContext())
            navController.popBackStack()
            return binding.root
        }
        scope = foundScope

        fileStorageViewModel.getProviderLiveData(scope, args.folderId, args.path?.toList()).observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                val foundFile = response.value.firstOrNull { it.file.id == args.fileId }
                if (foundFile == null) {
                    Reporter.reportException(R.string.error_file_not_found, args.fileId, requireContext())
                    navController.popBackStack()
                    return@observe
                }
                file = foundFile

                binding.fileName.text = file.file.name
                binding.fileCreatedAuthor.text = file.file.created.member.name
                binding.fileCreatedDate.text = getString(R.string.created_date).format(TextUtils.parseShortDate(file.file.created.date))
                binding.fileModifiedAuthor.text = file.file.getModified().member.name
                binding.fileModifiedAuthor.isVisible = file.file.created.member != file.file.getModified().member
                binding.fileModifiedDate.text = getString(R.string.modified_date).format(TextUtils.parseShortDate(file.file.getModified().date))
                binding.fileModifiedDate.isVisible = file.file.created.date != file.file.getModified().date

                when (file.file.type) {
                    FileType.FILE -> binding.fileSize.text = getString(R.string.size).format(Formatter.formatFileSize(requireContext(), file.file.getSize()))
                    FileType.FOLDER -> binding.fileSize.text = getString(R.string.children_count).format(file.children.value?.valueOrNull()?.size?.toString() ?: getString(R.string.unknown))
                }
                binding.fileIsMine.isChecked = file.file.isMine() == true
                binding.fileIsShared.isChecked = file.file.isShared() == true
                binding.fileIsSparse.isChecked = file.file.isSparseFile() == true

                binding.filePermissionReadable.isChecked = file.file.isReadable() == true
                binding.filePermissionWritable.isChecked = file.file.isWritable() == true
                binding.filePermissionEffectiveRead.isChecked = file.file.effectiveRead() == true
                binding.filePermissionEffectiveCreate.isChecked = file.file.effectiveCreate() == true
                binding.filePermissionEffectiveModify.isChecked = file.file.effectiveModify() == true
                binding.filePermissionEffectiveDelete.isChecked = file.file.effectiveDelete() == true

                binding.fileNotifications.isVisible = file.file.getDownloadNotification() != null
                binding.fileSelfDownloadNotification.isChecked = file.file.getDownloadNotification()?.me == true
                binding.fileDownloadNotificationListDescription.isVisible = file.file.getDownloadNotification()?.users?.isNotEmpty() == true
                binding.fileDownloadNotificationList.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, file.file.getDownloadNotification()?.users?.map { it.alias ?: it.name } ?: emptyList())

                binding.fileDescription.text = file.file.getDescription() ?: ""
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_files_failed, response.exception, requireContext())
                navController.popBackStack()
            }
        }

        binding.fabEditFile.setOnClickListener {
            //TODO implement
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                binding.fabEditFile.isVisible = false
                //TODO implement
                //binding.fabEditFile.isVisible = file.file.effectiveModify() == true
            } else {
                navController.popBackStack()
            }
        }

        return binding.root
    }

}