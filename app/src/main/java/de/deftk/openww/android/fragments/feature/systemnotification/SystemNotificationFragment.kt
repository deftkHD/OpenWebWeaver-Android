package de.deftk.openww.android.fragments.feature.systemnotification

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.activities.getMainActivity
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentSystemNotificationBinding
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.utils.UIUtil
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.feature.systemnotification.ISystemNotification
import java.text.DateFormat

class SystemNotificationFragment : Fragment() {

    private val args: SystemNotificationFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentSystemNotificationBinding
    private lateinit var systemNotification: ISystemNotification

    private var deleted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSystemNotificationBinding.inflate(inflater, container, false)
        getMainActivity().supportActionBar?.show()

        userViewModel.allSystemNotificationsResponse.observe(viewLifecycleOwner) { response ->
            getMainActivity().progressIndicator.isVisible = false
            if (deleted)
                return@observe

            if (response is Response.Success) {
                val foundSystemNotification = response.value.firstOrNull { it.id == args.systemNotificationId }
                if (foundSystemNotification == null) {
                    Reporter.reportException(R.string.error_system_notification_not_found, args.systemNotificationId, requireContext())
                    navController.popBackStack()
                    return@observe
                }
                systemNotification = foundSystemNotification
                binding.systemNotificationTitle.text = getString(UIUtil.getTranslatedSystemNotificationTitle(systemNotification))
                binding.systemNotificationAuthor.text = systemNotification.member.name
                binding.systemNotificationGroup.text = systemNotification.group.name
                binding.systemNotificationDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(systemNotification.date)
                binding.systemNotificationMessage.text = TextUtils.parseHtml(systemNotification.message)
                binding.systemNotificationMessage.movementMethod = LinkMovementMethod.getInstance()
                binding.systemNotificationMessage.transformationMethod = CustomTabTransformationMethod(binding.systemNotificationMessage.autoLinkMask)
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_system_notifications_failed, response.exception, requireContext())
            }
        }
        userViewModel.systemNotificationDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                userViewModel.resetDeleteResponse() // mark as handled
            getMainActivity().progressIndicator.isVisible = false

            if (response is Response.Success) {
                deleted = true
                navController.popBackStack()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }
        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext == null) {
                navController.popBackStack()
            }
        }

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.delete_menu_item, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_delete -> {
                val apiContext = userViewModel.apiContext.value ?: return false
                userViewModel.deleteSystemNotification(systemNotification, apiContext)
                getMainActivity().progressIndicator.isVisible = true
            }
            else -> return false
        }
        return true
    }

}