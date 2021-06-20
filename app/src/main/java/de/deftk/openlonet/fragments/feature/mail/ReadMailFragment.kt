package de.deftk.openlonet.fragments.feature.mail

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.mailbox.IEmail
import de.deftk.lonet.api.model.feature.mailbox.IEmailFolder
import de.deftk.openlonet.R
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.databinding.FragmentReadMailBinding
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.viewmodel.MailboxViewModel
import de.deftk.openlonet.viewmodel.UserViewModel
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
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mailboxViewModel.emailReadPostResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                mailboxViewModel.resetReadPostResponse() // mark as handled

            if (response is Response.Failure) {
                //TODO handle error
                response.exception.printStackTrace()
                binding.progressReadMail.isVisible = false
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
                //TODO handle error
                response.exception.printStackTrace()
            }
        }

        mailboxViewModel.foldersResponse.observe(viewLifecycleOwner) { resource ->
            if (resource is Response.Success) {
                emailFolder = resource.value.first { it.id == args.folderId }

                val response = mailboxViewModel.getCachedResponse(emailFolder)
                if (response is Response.Success) {
                    email = response.value.firstOrNull { it.id == args.mailId } ?: error("Referenced email not found")
                    binding.progressReadMail.isVisible = true
                    userViewModel.apiContext.value?.apply {
                        mailboxViewModel.readEmail(email, emailFolder, this)
                    }
                } else if (resource is Response.Failure) {
                    //TODO handle error
                    resource.exception.printStackTrace()
                }
            } else if (resource is Response.Failure) {
                //TODO handle error
                resource.exception.printStackTrace()
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
            if (apiContext.getUser().effectiveRights.contains(Permission.MAILBOX_ADMIN)) {
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