package de.deftk.openlonet.fragments.feature.board

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.ArrayAdapter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.lonet.api.model.IGroup
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.board.BoardNotificationColor
import de.deftk.lonet.api.model.feature.board.IBoardNotification
import de.deftk.openlonet.R
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.databinding.FragmentEditNotificationBinding
import de.deftk.openlonet.feature.board.BoardNotificationColors
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.viewmodel.BoardViewModel
import de.deftk.openlonet.viewmodel.UserViewModel

class EditNotificationFragment : Fragment() {

    //TODO implement board type
    //TODO implement kill date

    private val args: EditNotificationFragmentArgs by navArgs()
    private val boardViewModel: BoardViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentEditNotificationBinding
    private lateinit var notification: IBoardNotification
    private lateinit var group: IGroup

    private var editMode: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditNotificationBinding.inflate(inflater, container, false)

        val effectiveGroups = userViewModel.apiContext.value?.getUser()?.getGroups()?.filter { it.effectiveRights.contains(Permission.BOARD_ADMIN) } ?: emptyList()
        binding.notificationGroup.adapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, effectiveGroups.map { it.login })

        val colors = BoardNotificationColors.values()
        binding.notificationAccent.adapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, colors.map { getString(it.text) })

        if (args.groupId != null && args.notificationId != null) {
            // edit existing
            editMode = true
            boardViewModel.notificationsResponse.observe(viewLifecycleOwner) { resource ->
                if (resource is Response.Success) {
                    resource.value.firstOrNull { it.first.id == args.notificationId && it.second.login == args.groupId }?.apply {
                        notification = first
                        group = second

                        binding.notificationTitle.setText(notification.getTitle())
                        binding.notificationGroup.setSelection(effectiveGroups.indexOf(group))
                        binding.notificationGroup.isEnabled = false
                        binding.notificationAccent.setSelection(colors.indexOf(BoardNotificationColors.getByApiColor(notification.getColor() ?: BoardNotificationColor.BLUE)))
                        binding.notificationText.setText(TextUtils.parseInternalReferences(TextUtils.parseHtml(notification.getText())))
                        binding.notificationText.movementMethod = LinkMovementMethod.getInstance()
                        binding.notificationText.transformationMethod = CustomTabTransformationMethod(binding.notificationText.autoLinkMask)
                    }
                } else if (resource is Response.Failure) {
                    //TODO handle error
                    resource.exception.printStackTrace()
                }
            }
        } else {
            // add new
            editMode = false
            binding.notificationGroup.isEnabled = true
        }

        boardViewModel.postResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                boardViewModel.resetPostResponse() // mark as handled

            if (response is Response.Success) {
                ViewCompat.getWindowInsetsController(requireView())?.hide(WindowInsetsCompat.Type.ime())
                navController.popBackStack()
            } else if (response is Response.Failure) {
                //TODO handle error
                response.exception.printStackTrace()
            }
        }

        setHasOptionsMenu(true)
        return binding.root
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_save_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.save) {
            val apiContext = userViewModel.apiContext.value ?: return false

            val title = binding.notificationTitle.text.toString()
            val selectedGroup = binding.notificationGroup.selectedItem
            val color = BoardNotificationColors.values()[binding.notificationAccent.selectedItemPosition].apiColor
            val text = binding.notificationText.text.toString()

            if (editMode) {
                boardViewModel.editBoardNotification(notification, title, text, color, null, group, apiContext)
            } else {
                group = apiContext.getUser().getGroups().firstOrNull { it.login == selectedGroup } ?: return false
                boardViewModel.addBoardNotification(title, text, color, null, group, apiContext)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}