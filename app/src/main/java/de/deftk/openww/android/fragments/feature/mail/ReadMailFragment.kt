package de.deftk.openww.android.fragments.feature.mail

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.mailbox.IEmail
import de.deftk.openww.api.model.feature.mailbox.IEmailFolder
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentReadMailBinding
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.MailboxViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import java.text.DateFormat

class ReadMailFragment : Fragment() {

    private val args: ReadMailFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val mailboxViewModel: MailboxViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentReadMailBinding
    private lateinit var email: IEmail
    private lateinit var emailFolder: IEmailFolder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadMailBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mailboxViewModel.emailReadPostResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                mailboxViewModel.resetReadPostResponse() // mark as handled

            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_read_email_failed, response.exception, requireContext())
                binding.progressReadMail.isVisible = false
                navController.popBackStack()
                return@observe
            } else if (response is Response.Success) {
                binding.progressReadMail.isVisible = false
                response.value?.also { email ->
                    binding.mailSubject.text = email.getSubject()
                    binding.mailAuthor.text = (email.getFrom() ?: emptyList()).firstOrNull()?.name ?: ""
                    binding.mailAuthorAddress.text = (email.getFrom() ?: emptyList()).firstOrNull()?.address ?: ""
                    binding.mailDate.text = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.DEFAULT).format(email.getDate())
                    val text = email.getText() ?: email.getPlainBody()
                    binding.mailMessage.text = TextUtils.parseMultipleQuotes(TextUtils.parseInternalReferences(TextUtils.parseHtml(text)))
                    binding.mailMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.mailMessage.transformationMethod = CustomTabTransformationMethod(binding.mailMessage.autoLinkMask)
                }

            }
        }

        mailboxViewModel.emailPostResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                mailboxViewModel.resetPostResponse() // mark as handled

            if (response is Response.Success) {
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
                    binding.progressReadMail.isVisible = true
                    userViewModel.apiContext.value?.apply {
                        mailboxViewModel.readEmail(email, emailFolder, this)
                    }
                } else if (mailResponse is Response.Failure) {
                    Reporter.reportException(R.string.error_get_emails_failed, mailResponse.exception, requireContext())
                }
            } else if (folderResponse is Response.Failure) {
                Reporter.reportException(R.string.error_get_folders_failed, folderResponse.exception, requireContext())
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext == null) {
                navController.popBackStack(R.id.mailFragment, false)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        userViewModel.apiContext.value?.also { apiContext ->
            if (apiContext.getUser().effectiveRights.contains(Permission.MAILBOX_WRITE) || apiContext.getUser().effectiveRights.contains(Permission.MAILBOX_ADMIN)) {
                inflater.inflate(R.menu.simple_mail_edit_item_menu, menu)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_delete -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    mailboxViewModel.deleteEmail(email, emailFolder, true, apiContext)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}