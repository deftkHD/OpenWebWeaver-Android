package de.deftk.openww.android.fragments.feature.board

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentEditNotificationBinding
import de.deftk.openww.android.feature.board.BoardNotificationColors
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.BoardViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.board.BoardNotificationColor
import de.deftk.openww.api.model.feature.board.IBoardNotification

class EditNotificationFragment : AbstractFragment(true) {

    //TODO implement board type
    //TODO implement kill date

    private val args: EditNotificationFragmentArgs by navArgs()
    private val boardViewModel: BoardViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentEditNotificationBinding
    private lateinit var notification: IBoardNotification
    private lateinit var group: IGroup

    private var effectiveGroups: List<IGroup>? = null
    private var colors: Array<BoardNotificationColors>? = null
    private var editMode: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditNotificationBinding.inflate(inflater, container, false)

        boardViewModel.allNotificationsResponse.observe(viewLifecycleOwner) { response ->
            enableUI(true)
            if (response is Response.Success) {
                if (args.notificationId != null && args.groupId != null) {
                    // edit existing
                    editMode = true
                    val searched = response.value.firstOrNull { it.first.id == args.notificationId && it.second.login == args.groupId }
                    if (searched == null) {
                        Reporter.reportException(R.string.error_notification_not_found, args.notificationId!!, requireContext())
                        navController.popBackStack()
                        return@observe
                    }

                    notification = searched.first
                    group = searched.second

                    binding.notificationTitle.setText(notification.title)
                    if (effectiveGroups != null)
                        binding.notificationGroup.setSelection(effectiveGroups!!.indexOf(group))
                    binding.notificationGroup.isEnabled = false
                    if (colors != null)
                        binding.notificationAccent.setSelection(colors!!.indexOf(BoardNotificationColors.getByApiColor(notification.color ?: BoardNotificationColor.BLUE)))
                    binding.notificationText.setText(notification.text)
                } else {
                    // add new
                    editMode = false
                    binding.notificationGroup.isEnabled = true
                }
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_notifications_failed, response.exception, requireContext())
                navController.popBackStack()
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (apiContext.user.getGroups().none { it.effectiveRights.contains(Permission.BOARD_WRITE) } && apiContext.user.getGroups().none { it.effectiveRights.contains(Permission.BOARD_ADMIN) }) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }

                effectiveGroups = apiContext.user.getGroups().filter { it.effectiveRights.contains(Permission.BOARD_WRITE) || it.effectiveRights.contains(Permission.BOARD_ADMIN) }
                binding.notificationGroup.adapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, effectiveGroups!!.map { it.login })

                colors = BoardNotificationColors.values()
                binding.notificationAccent.adapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, colors!!.map { getString(it.text) })

                boardViewModel.loadBoardNotifications(apiContext)
                if (boardViewModel.allNotificationsResponse.value == null)
                    enableUI(false)
            } else {
                binding.notificationTitle.setText("")
                binding.notificationGroup.adapter = null
                binding.notificationGroup.isEnabled = false
                binding.notificationAccent.adapter = null
                binding.notificationText.setText("")
                enableUI(false)
            }
        }

        boardViewModel.postResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                boardViewModel.resetPostResponse() // mark as handled
            enableUI(true)

            if (response is Response.Success) {
                ViewCompat.getWindowInsetsController(requireView())?.hide(WindowInsetsCompat.Type.ime())
                navController.popBackStack()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_save_changes_failed, response.exception, requireContext())
            }
        }

        setHasOptionsMenu(true)
        return binding.root
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.edit_options_item_save) {
            val apiContext = userViewModel.apiContext.value ?: return false

            val title = binding.notificationTitle.text.toString()
            val selectedGroup = binding.notificationGroup.selectedItem
            val color = BoardNotificationColors.values()[binding.notificationAccent.selectedItemPosition].apiColor
            val text = binding.notificationText.text.toString()

            if (editMode) {
                boardViewModel.editBoardNotification(notification, title, text, color, null, group, apiContext)
                enableUI(false)
            } else {
                group = apiContext.user.getGroups().firstOrNull { it.login == selectedGroup } ?: return false
                boardViewModel.addBoardNotification(title, text, color, null, group, apiContext)
                enableUI(false)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onUIStateChanged(enabled: Boolean) {
        binding.notificationAccent.isEnabled = enabled
        if (!editMode)
            binding.notificationGroup.isEnabled = enabled
        binding.notificationText.isEnabled = enabled
        binding.notificationTitle.isEnabled = enabled
    }
}