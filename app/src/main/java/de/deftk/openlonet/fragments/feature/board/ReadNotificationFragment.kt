package de.deftk.openlonet.fragments.feature.board

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.lonet.api.model.IGroup
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.board.IBoardNotification
import de.deftk.openlonet.R
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.databinding.FragmentReadNotificationBinding
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.viewmodel.BoardViewModel
import de.deftk.openlonet.viewmodel.UserViewModel
import java.text.DateFormat

class ReadNotificationFragment : Fragment() {

    private val args: ReadNotificationFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val boardViewModel: BoardViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentReadNotificationBinding
    private lateinit var notification: IBoardNotification
    private lateinit var group: IGroup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadNotificationBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        boardViewModel.notificationsResponse.observe(viewLifecycleOwner) { resource ->
            if (resource is Response.Success) {
                resource.value.firstOrNull { it.first.id == args.notificationId && it.second.login == args.groupId }?.apply {
                    notification = first
                    group = second

                    binding.notificationTitle.text = notification.getTitle()
                    binding.notificationAuthor.text = notification.created.member.name
                    binding.notificationGroup.text = group.name
                    binding.notificationDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(notification.created.date)
                    binding.notificationText.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(notification.getText()))
                    binding.notificationText.movementMethod = LinkMovementMethod.getInstance()
                    binding.notificationText.transformationMethod = CustomTabTransformationMethod(binding.notificationText.autoLinkMask)

                    if (group.effectiveRights.contains(Permission.BOARD_ADMIN)) {
                        binding.fabEditNotification.isVisible = true
                        binding.fabEditNotification.setOnClickListener {
                            val action = ReadNotificationFragmentDirections.actionReadNotificationFragmentToEditNotificationFragment(notification.id, group.login, getString(R.string.edit_notification))
                            navController.navigate(action)
                        }
                    }
                }
            } else if (resource is Response.Failure) {
                resource.exception.printStackTrace()
                //TODO handle error
            }
        }
        boardViewModel.postResponse.observe(viewLifecycleOwner) { result ->
            if (result != null)
                boardViewModel.resetPostResponse() // mark as handled

            if (result is Response.Success) {
                navController.popBackStack()
            } else if (result is Response.Failure) {
                //TODO handle error
                result.exception.printStackTrace()
            }
        }
        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext == null) {
                navController.popBackStack(R.id.notificationsFragment, false)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (group.effectiveRights.contains(Permission.BOARD_ADMIN))
            inflater.inflate(R.menu.simple_edit_item_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                val action = ReadNotificationFragmentDirections.actionReadNotificationFragmentToEditNotificationFragment(notification.id, group.login, getString(R.string.edit_notification))
                navController.navigate(action)
                true
            }
            R.id.menu_item_delete -> {
                val apiContext = userViewModel.apiContext.value ?: return false
                boardViewModel.deleteBoardNotification(notification, group, apiContext)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}