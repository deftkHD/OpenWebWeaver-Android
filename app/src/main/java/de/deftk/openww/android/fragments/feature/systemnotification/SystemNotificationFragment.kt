package de.deftk.openww.android.fragments.feature.systemnotification

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.api.model.feature.systemnotification.ISystemNotification
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentSystemNotificationBinding
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.utils.UIUtil
import de.deftk.openww.android.viewmodel.UserViewModel
import java.text.DateFormat

class SystemNotificationFragment : Fragment() {

    private val args: SystemNotificationFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var binding: FragmentSystemNotificationBinding
    private lateinit var systemNotification: ISystemNotification

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSystemNotificationBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        userViewModel.systemNotificationsResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                response.value.firstOrNull { it.id == args.systemNotificationId }?.apply {
                    systemNotification = this

                    binding.systemNotificationTitle.text = getString(UIUtil.getTranslatedSystemNotificationTitle(systemNotification))
                    binding.systemNotificationAuthor.text = member.name
                    binding.systemNotificationGroup.text = group.name
                    binding.systemNotificationDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(date)
                    binding.systemNotificationMessage.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(message))
                    binding.systemNotificationMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.systemNotificationMessage.transformationMethod = CustomTabTransformationMethod(binding.systemNotificationMessage.autoLinkMask)
                }
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_login_failed, response.exception, requireContext())
            }
        }
        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext == null) {
                findNavController().popBackStack(R.id.systemNotificationsFragment, false)
            }
        }
    }

}