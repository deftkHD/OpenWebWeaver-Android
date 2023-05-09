package de.deftk.openww.android.fragments.feature.board

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.FragmentReadNotificationBinding
import de.deftk.openww.android.exception.ObjectNotFoundException
import de.deftk.openww.android.fragments.ContextualFragment
import de.deftk.openww.android.fragments.feature.board.viewmodel.ReadNotificationFragmentUIState
import de.deftk.openww.android.fragments.feature.board.viewmodel.ReadNotificationViewModel
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import kotlinx.coroutines.launch
import java.text.DateFormat

@AndroidEntryPoint
class ReadNotificationFragment : ContextualFragment(true) {

    private val viewModel by viewModels<ReadNotificationViewModel>()

    private lateinit var binding: FragmentReadNotificationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadNotificationBinding.inflate(inflater, container, false)

        binding.fabEditNotification.setOnClickListener {
            viewModel.editNotification(navController)
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

                            binding.fabEditNotification.isVisible = viewModel.canEdit()

                            setUIState(UIState.READY)
                        }
                        is ReadNotificationFragmentUIState.Failure -> {
                            setUIState(UIState.ERROR)
                            if (uiState.throwable is ObjectNotFoundException) {
                                Reporter.reportException(R.string.error_notification_not_found, viewModel.notificationId.toString(), requireContext())
                                navController.popBackStack()
                            } else {
                                Reporter.reportException(R.string.error_other, uiState.throwable, requireContext())
                                navController.popBackStack()
                            }
                        }
                        is ReadNotificationFragmentUIState.Closed -> {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        lifecycleScope.launch {
            if (viewModel.canEdit()) {
                menuInflater.inflate(R.menu.board_context_menu, menu)
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.board_context_item_edit -> {
                viewModel.editNotification(navController)
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