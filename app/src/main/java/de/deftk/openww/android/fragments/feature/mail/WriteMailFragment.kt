package de.deftk.openww.android.fragments.feature.mail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentWriteMailBinding
import de.deftk.openww.android.feature.LaunchMode
import de.deftk.openww.android.viewmodel.MailboxViewModel
import de.deftk.openww.android.viewmodel.UserViewModel

class WriteMailFragment : Fragment() {

    //TODO attachments
    //TODO reply emails
    //TODO forward emails
    //TODO account switch if launchMode == EMAIL

    private val userViewModel: UserViewModel by activityViewModels()
    private val mailboxViewModel: MailboxViewModel by activityViewModels()
    private val launchMode by lazy { LaunchMode.getLaunchMode(requireActivity().intent) }

    private lateinit var binding: FragmentWriteMailBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentWriteMailBinding.inflate(inflater, container, false)

        (requireActivity() as AppCompatActivity).supportActionBar?.show()

        val intent = requireActivity().intent
        binding.mailToAddress.setText(intent.getStringExtra(Intent.EXTRA_EMAIL) ?: "")
        binding.mailToAddressCc.setText(intent.getStringExtra(Intent.EXTRA_CC) ?: "")
        binding.mailToAddressBcc.setText(intent.getStringExtra(Intent.EXTRA_BCC) ?: "")
        binding.mailSubject.setText(intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "")
        binding.mailMessage.setText(intent.getStringExtra(Intent.EXTRA_TEXT) ?: "")

        if (intent.data != null) {
            val uri = intent.data!!
            binding.mailToAddress.setText(uri.schemeSpecificPart ?: "")
        }

        mailboxViewModel.emailSendResponse.observe(viewLifecycleOwner) { response ->
            binding.progressSendMail.isVisible = false
            if (response is Response.Success) {
                Toast.makeText(requireContext(), R.string.email_sent, Toast.LENGTH_LONG).show()
                if (launchMode == LaunchMode.DEFAULT) {
                    findNavController().popBackStack()
                } else if (launchMode == LaunchMode.EMAIL) {
                    requireActivity().finish()
                }
            } else if (response is Response.Failure) {
                //TODO handle error
                response.exception.printStackTrace()
            }
        }

        binding.fabSendMail.setOnClickListener {
            val subject = binding.mailSubject.text.toString()
            val message = binding.mailMessage.text.toString()
            val to = binding.mailToAddress.text.toString()
            val toCC = binding.mailToAddressCc.text.toString()
            val toBCC = binding.mailToAddressBcc.text.toString()
            if (subject.isEmpty()) {
                Toast.makeText(requireContext(), R.string.mail_no_subject, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (message.isEmpty()) {
                Toast.makeText(requireContext(), R.string.mail_no_message, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (to.isEmpty()) {
                Toast.makeText(requireContext(), R.string.mail_no_to, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            binding.progressSendMail.isVisible = true
            userViewModel.apiContext.value?.also { apiContext ->
                mailboxViewModel.sendEmail(
                    to,
                    subject,
                    message,
                    toCC.ifBlank { null },
                    toBCC.ifBlank { null },
                    null,
                    null,
                    null,
                    null,
                    null,
                    apiContext
                )
            }
        }

        return binding.root
    }

}