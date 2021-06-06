package de.deftk.openlonet.fragments.feature.systemnotification

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import de.deftk.lonet.api.model.feature.systemnotification.ISystemNotification
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.databinding.FragmentSystemNotificationBinding
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.UIUtil
import de.deftk.openlonet.viewmodel.UserViewModel
import java.text.DateFormat

class SystemNotificationFragment : Fragment() {

    private val args: SystemNotificationFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var binding: FragmentSystemNotificationBinding
    private lateinit var systemNotification: ISystemNotification

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSystemNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        userViewModel.systemNotifications.observe(viewLifecycleOwner) { resource ->
            if (resource is Response.Success) {
                resource.value.firstOrNull { it.id == args.systemNotificationId }?.apply {
                    systemNotification = this

                    binding.systemNotificationTitle.text = getString(UIUtil.getTranslatedSystemNotificationTitle(systemNotification))
                    binding.systemNotificationAuthor.text = member.name
                    binding.systemNotificationGroup.text = group.name
                    binding.systemNotificationDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(date)
                    binding.systemNotificationMessage.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(message))
                    binding.systemNotificationMessage.movementMethod = LinkMovementMethod.getInstance()
                    binding.systemNotificationMessage.transformationMethod = CustomTabTransformationMethod(binding.systemNotificationMessage.autoLinkMask)
                }
            } else {
                //TODO report error
            }
        }
    }

}