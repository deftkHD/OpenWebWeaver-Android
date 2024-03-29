package de.deftk.openww.android.fragments.feature.mail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.work.WorkInfo
import androidx.work.WorkManager
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.AttachmentAdapter
import de.deftk.openww.android.adapter.recycler.AttachmentClickListener
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentReadMailBinding
import de.deftk.openww.android.feature.AbstractNotifyingWorker
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.FileUtil
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.MailboxViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.mailbox.IAttachment
import de.deftk.openww.api.model.feature.mailbox.IEmail
import de.deftk.openww.api.model.feature.mailbox.IEmailFolder
import java.text.DateFormat

class ReadMailFragment : AbstractFragment(true), AttachmentClickListener {

    private val args: ReadMailFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val mailboxViewModel: MailboxViewModel by activityViewModels()
    private val navController by lazy { findNavController() }
    private val workManager by lazy { WorkManager.getInstance(requireContext()) }

    private lateinit var binding: FragmentReadMailBinding
    private lateinit var email: IEmail
    private lateinit var emailFolder: IEmailFolder
    private lateinit var downloadSaveLauncher: ActivityResultLauncher<Pair<Intent, IAttachment>>

    private var deleted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadMailBinding.inflate(inflater, container, false)

        mailboxViewModel.emailReadPostResponse.observe(viewLifecycleOwner) { response ->
            enableUI(true)
            if (response != null)
                mailboxViewModel.resetReadPostResponse() // mark as handled
            if (deleted)
                return@observe

            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_read_email_failed, response.exception, requireContext())
                navController.popBackStack()
                return@observe
            } else if (response is Response.Success) {
                response.value?.also { email ->
                    binding.mailSubject.text = email.subject
                    binding.mailAuthor.text = (email.from ?: emptyList()).firstOrNull()?.name ?: ""
                    binding.mailAuthorAddress.text = (email.from ?: emptyList()).firstOrNull()?.address ?: ""
                    binding.mailDate.text = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.DEFAULT).format(email.date)
                    val text = email.text ?: email.plainBody
                    binding.mailMessage.text = TextUtils.parseMultipleQuotes(TextUtils.parseInternalReferences(TextUtils.parseHtml(text), null, navController))
                    binding.mailMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.mailMessage.transformationMethod = CustomTabTransformationMethod(binding.mailMessage.autoLinkMask)

                    val hasAttachments = email.attachments?.isNotEmpty() == true
                    binding.containerAttachments.isVisible = hasAttachments
                    if (hasAttachments) {
                        val attachmentText = resources.getQuantityText(R.plurals.attachments_num, email.attachments!!.size).toString().format(email.attachments!!.size)
                        binding.btnAttachments.text = attachmentText
                        binding.btnAttachments.textOff = attachmentText
                        binding.btnAttachments.textOn = attachmentText
                        binding.btnAttachments.setOnClickListener {
                            binding.attachmentList.isVisible = !binding.attachmentList.isVisible
                        }
                        val adapter = AttachmentAdapter(this)
                        binding.attachmentList.adapter = adapter
                        adapter.submitList(email.attachments)
                    }
                }
            }
        }

        mailboxViewModel.emailPostResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                mailboxViewModel.resetPostResponse() // mark as handled

            if (response is Response.Success) {
                deleted = true
                navController.popBackStack()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_save_changes_failed, response.exception, requireContext())
            }
        }

        mailboxViewModel.foldersResponse.observe(viewLifecycleOwner) { folderResponse ->
            if (folderResponse is Response.Success) {
                emailFolder = folderResponse.value.first { it.id == args.folderId }

                val mailResponse = mailboxViewModel.getCachedResponse(emailFolder)
                if (mailResponse is Response.Success) {
                    val foundEmail = mailResponse.value.firstOrNull { it.id == args.mailId }
                    if (foundEmail == null) {
                        Reporter.reportException(R.string.error_email_not_found, args.mailId.toString(), requireContext())
                        navController.popBackStack()
                        return@observe
                    }
                    email = foundEmail
                    userViewModel.apiContext.value?.apply {
                        mailboxViewModel.readEmail(email, emailFolder, this)
                        enableUI(false)
                    }
                } else if (mailResponse is Response.Failure) {
                    Reporter.reportException(R.string.error_get_emails_failed, mailResponse.exception, requireContext())
                }
            } else if (folderResponse is Response.Failure) {
                Reporter.reportException(R.string.error_get_folders_failed, folderResponse.exception, requireContext())
            }
        }

        mailboxViewModel.exportSessionFileResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                mailboxViewModel.resetExportAttachmentResponse() // mark as handled

            if (response is Response.Success) {
                FileUtil.openAttachment(this, response.value, "attachment")
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_download_worker_failed, response.exception, requireContext())
            }
        }

        mailboxViewModel.downloadSaveAttachmentWorkerId.observe(viewLifecycleOwner) { uuid ->
            if (uuid == null)
                return@observe


            workManager.getWorkInfoByIdLiveData(uuid).observe(viewLifecycleOwner) { workInfo ->
                fun reset() {
                    if (mailboxViewModel.downloadSaveAttachmentWorkerId.value == uuid) {
                        mailboxViewModel.resetDownloadSaveAttachmentWorkerId()
                    }
                }
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        reset()
                    }
                    WorkInfo.State.FAILED -> {
                        val message = workInfo.outputData.getString(AbstractNotifyingWorker.DATA_ERROR_MESSAGE) ?: "Unknown"
                        Reporter.reportException(R.string.error_download_worker_failed, message, requireContext())
                        reset()
                    }
                    else -> { /* ignore */ }
                }
            }
        }

        downloadSaveLauncher = registerForActivityResult(SaveAttachmentContract()) { (result, attachment) ->
            val uri = result.data?.data
            userViewModel.apiContext.value?.also { apiContext ->
                mailboxViewModel.startAttachmentSaveDownload(workManager, apiContext, attachment, email, emailFolder, uri.toString())
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext == null) {
                navController.popBackStack(R.id.mailFragment, false)
            }
        }

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        userViewModel.apiContext.value?.also { apiContext ->
            if (apiContext.user.effectiveRights.contains(Permission.MAILBOX_WRITE) || apiContext.user.effectiveRights.contains(Permission.MAILBOX_ADMIN)) {
                menuInflater.inflate(R.menu.mail_context_menu, menu)
                menu.findItem(R.id.mail_context_item_move).isVisible = false
                menu.findItem(R.id.mail_context_item_set_read).isVisible = false
                menu.findItem(R.id.mail_context_item_set_unread).isVisible = false
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.mail_context_item_delete -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    mailboxViewModel.deleteEmail(email, emailFolder, true, apiContext)
                    enableUI(false)
                }
                true
            }
            else -> false
        }
    }

    override fun onUIStateChanged(enabled: Boolean) {}

    override fun onSaveAttachment(attachment: IAttachment) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = FileUtil.getMimeType(attachment.name)
        intent.putExtra(Intent.EXTRA_TITLE, attachment.name)
        downloadSaveLauncher.launch(intent to attachment)
    }

    override fun onOpenAttachment(attachment: IAttachment) {
        userViewModel.apiContext.value?.also { apiContext ->
            mailboxViewModel.exportAttachment(attachment, email, emailFolder, apiContext)
        }
    }
}

class SaveAttachmentContract : ActivityResultContract<Pair<Intent, IAttachment>, Pair<ActivityResult, IAttachment>>() {

    private lateinit var attachment: IAttachment

    override fun createIntent(context: Context, input: Pair<Intent, IAttachment>): Intent {
        attachment = input.second
        return input.first
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Pair<ActivityResult, IAttachment> {
        return Pair(ActivityResult(resultCode, intent), attachment)
    }
}