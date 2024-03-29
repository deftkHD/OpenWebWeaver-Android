package de.deftk.openww.android.fragments.feature.board

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentReadNotificationBinding
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.BoardViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.board.IBoardNotification
import java.text.DateFormat

class ReadNotificationFragment : AbstractFragment(true) {

    private val args: ReadNotificationFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val boardViewModel: BoardViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentReadNotificationBinding
    private lateinit var notification: IBoardNotification
    private lateinit var group: IGroup

    private var deleted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadNotificationBinding.inflate(inflater, container, false)

        boardViewModel.allNotificationsResponse.observe(viewLifecycleOwner) { response ->
            enableUI(true)
            if (deleted)
                return@observe

            if (response is Response.Success) {
                val searched = response.value.firstOrNull { it.first.id == args.notificationId && it.second.login == args.groupId }
                if (searched == null) {
                    Reporter.reportException(R.string.error_notification_not_found, args.notificationId, requireContext())
                    navController.popBackStack()
                    return@observe
                }

                notification = searched.first
                group = searched.second

                binding.notificationTitle.text = notification.title
                binding.notificationAuthor.text = notification.created.member.name
                binding.notificationGroup.text = group.name
                binding.notificationDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(notification.created.date)
                binding.notificationText.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(notification.text), group.login, navController)
                binding.notificationText.movementMethod = LinkMovementMethod.getInstance()
                binding.notificationText.transformationMethod = CustomTabTransformationMethod(binding.notificationText.autoLinkMask)

                binding.fabEditNotification.isVisible = group.effectiveRights.contains(Permission.BOARD_ADMIN)
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_notifications_failed, response.exception, requireContext())
                navController.popBackStack()
                return@observe
            }
        }
        binding.fabEditNotification.setOnClickListener {
            val action = ReadNotificationFragmentDirections.actionReadNotificationFragmentToEditNotificationFragment(notification.id, group.login, getString(R.string.edit_notification))
            navController.navigate(action)
        }
        boardViewModel.postResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                boardViewModel.resetPostResponse() // mark as handled
            enableUI(true)

            if (response is Response.Success) {
                deleted = true
                navController.popBackStack()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }
        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (apiContext.user.getGroups().none { Feature.BOARD.isAvailable(it.effectiveRights) }) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }
                boardViewModel.loadBoardNotifications(apiContext)
                if (boardViewModel.allNotificationsResponse.value == null)
                    enableUI(false)
            } else {
                binding.notificationTitle.text = ""
                binding.notificationAuthor.text = ""
                binding.notificationGroup.text = ""
                binding.notificationDate.text = ""
                binding.notificationText.text = ""
                binding.fabEditNotification.isVisible = false
                enableUI(false)
            }
        }

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (group.effectiveRights.contains(Permission.BOARD_WRITE) || group.effectiveRights.contains(Permission.BOARD_ADMIN))
            menuInflater.inflate(R.menu.board_context_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.board_context_item_edit -> {
                val action = ReadNotificationFragmentDirections.actionReadNotificationFragmentToEditNotificationFragment(notification.id, group.login, getString(R.string.edit_notification))
                navController.navigate(action)
                true
            }
            R.id.board_context_item_delete -> {
                val apiContext = userViewModel.apiContext.value ?: return false
                boardViewModel.deleteBoardNotification(notification, group, apiContext)
                enableUI(false)
                true
            }
            else -> false
        }
    }

    override fun onUIStateChanged(enabled: Boolean) {
        binding.fabEditNotification.isEnabled = enabled
    }
}