package de.deftk.openww.android.fragments.feature.board

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.FragmentReadNotificationBinding
import de.deftk.openww.android.fragments.ContextualFragment
import de.deftk.openww.android.fragments.feature.board.viewmodel.ReadNotificationFragmentUIState
import de.deftk.openww.android.fragments.feature.board.viewmodel.ReadNotificationViewModel
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.api.model.Permission
import kotlinx.coroutines.launch
import java.text.DateFormat

@AndroidEntryPoint
class ReadNotificationFragment : ContextualFragment(true) {

    //TODO what happens when notification not found

    private val args: ReadNotificationFragmentArgs by navArgs()
    private val viewModel by viewModels<ReadNotificationViewModel>()

    private lateinit var binding: FragmentReadNotificationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadNotificationBinding.inflate(inflater, container, false)

        viewModel.setNotification(args.notificationId, args.groupId)

        binding.fabEditNotification.setOnClickListener {
            viewModel.navigateEditNotification(navController)
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    when (uiState) {
                        is ReadNotificationFragmentUIState.Loading -> {
                            setUIState(UIState.LOADING)
                        }
                        is ReadNotificationFragmentUIState.Success -> {
                            val notification = uiState.notification.notification
                            val group = uiState.notification.group
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

                            setUIState(UIState.READY)
                        }
                        is ReadNotificationFragmentUIState.Failure -> {
                            setUIState(UIState.ERROR)
                        }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        lifecycleScope.launch {
            val notification = viewModel.getNotification()
            if (notification != null) {
                val group = notification.group
                if (group.effectiveRights.contains(Permission.BOARD_WRITE) || group.effectiveRights.contains(Permission.BOARD_ADMIN)) {
                    menuInflater.inflate(R.menu.board_context_menu, menu)
                }
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.board_context_item_edit -> {
                viewModel.navigateEditNotification(navController)
                true
            }
            R.id.board_context_item_delete -> {
                viewModel.deleteNotification()
                true
            }
            else -> false
        }
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.fabEditNotification.isEnabled = newState == UIState.READY
    }
}