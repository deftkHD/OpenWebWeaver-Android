package de.deftk.openww.android.fragments.feature.board

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentReadNotificationBinding
import de.deftk.openww.android.fragments.ContextualFragment
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.BoardViewModel
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.board.IBoardNotification
import java.text.DateFormat

class ReadNotificationFragment : ContextualFragment(true) {

    private val args: ReadNotificationFragmentArgs by navArgs()
    private val boardViewModel: BoardViewModel by activityViewModels()

    private lateinit var binding: FragmentReadNotificationBinding

    private var notification: IBoardNotification? = null
    private var group: IGroup? = null
    private var deleted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadNotificationBinding.inflate(inflater, container, false)

        boardViewModel.allNotificationsResponse.observe(viewLifecycleOwner) { response ->
            if (deleted)
                return@observe

            if (response is Response.Success) {
                setUIState(UIState.READY)
                val searched = response.value.firstOrNull { it.first.id == args.notificationId && it.second.login == args.groupId }
                if (searched == null) {
                    Reporter.reportException(R.string.error_notification_not_found, args.notificationId, requireContext())
                    navController.popBackStack()
                    return@observe
                }

                notification = searched.first
                group = searched.second
                val notification = searched.first
                val group = searched.second

                binding.notificationTitle.text = notification.title
                binding.notificationAuthor.text = notification.created.member.name
                binding.notificationGroup.text = group.name
                if (notification.created.date != null) {
                    binding.notificationDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(notification.created.date!!)
                } else {
                    binding.notificationDate.isVisible = false
                }
                binding.notificationText.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(notification.text), group.login, navController)
                binding.notificationText.movementMethod = LinkMovementMethod.getInstance()
                binding.notificationText.transformationMethod = CustomTabTransformationMethod(binding.notificationText.autoLinkMask)

                binding.fabEditNotification.isVisible = group.effectiveRights.contains(Permission.BOARD_ADMIN)
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_get_notifications_failed, response.exception, requireContext())
                navController.popBackStack()
            }
            invalidateOptionsMenu()
        }
        binding.fabEditNotification.setOnClickListener {
            if (notification != null) {
                val action = ReadNotificationFragmentDirections.actionReadNotificationFragmentToEditNotificationFragment(notification!!.id, group!!.login)
                navController.navigate(action)
            }
        }
        boardViewModel.postResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                boardViewModel.resetPostResponse() // mark as handled

            if (response is Response.Success) {
                setUIState(UIState.READY)
                deleted = true
                navController.popBackStack()
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }
        loginViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (apiContext.user.getGroups().none { Feature.BOARD.isAvailable(it.effectiveRights) }) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }
                if (boardViewModel.allNotificationsResponse.value == null) {
                    boardViewModel.loadBoardNotifications(apiContext)
                    setUIState(UIState.LOADING)
                }
            } else {
                binding.notificationTitle.text = ""
                binding.notificationAuthor.text = ""
                binding.notificationGroup.text = ""
                binding.notificationDate.text = ""
                binding.notificationText.text = ""
                setUIState(UIState.DISABLED)
            }
        }

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (group != null) {
            if (group!!.effectiveRights.contains(Permission.BOARD_WRITE) || group!!.effectiveRights.contains(Permission.BOARD_ADMIN))
                menuInflater.inflate(R.menu.board_context_menu, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.board_context_item_edit -> {
                if (notification != null && group != null) {
                    val action = ReadNotificationFragmentDirections.actionReadNotificationFragmentToEditNotificationFragment(notification!!.id, group!!.login)
                    navController.navigate(action)
                }
                true
            }
            R.id.board_context_item_delete -> {
                if (notification != null && group != null) {
                    val apiContext = loginViewModel.apiContext.value ?: return false
                    boardViewModel.deleteBoardNotification(notification!!, group!!, apiContext)
                    setUIState(UIState.LOADING)
                }
                true
            }
            else -> false
        }
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.fabEditNotification.isEnabled = newState == UIState.READY
    }
}