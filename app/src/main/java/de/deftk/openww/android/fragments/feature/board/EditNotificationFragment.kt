package de.deftk.openww.android.fragments.feature.board

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.ScopeSelectionAdapter
import de.deftk.openww.android.databinding.FragmentEditNotificationBinding
import de.deftk.openww.android.feature.board.BoardNotificationColors
import de.deftk.openww.android.fragments.ContextualFragment
import de.deftk.openww.android.fragments.feature.board.viewmodel.EditNotificationFragmentUIState
import de.deftk.openww.android.fragments.feature.board.viewmodel.EditNotificationViewModel
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.board.BoardNotificationColor
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditNotificationFragment : ContextualFragment(true) {

    //FIXME this whole fragment is broken

    //TODO implement board type
    //TODO implement kill date

    private val args: EditNotificationFragmentArgs by navArgs()
    private val viewModel by viewModels<EditNotificationViewModel>()

    private lateinit var binding: FragmentEditNotificationBinding

    private var effectiveGroups: List<IGroup>? = null
    private var colors: Array<BoardNotificationColors>? = null
    private var editMode: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditNotificationBinding.inflate(inflater, container, false)

        if (args.notificationId != null && args.groupId != null) {
            editMode = true
            viewModel.setNotification(args.notificationId!!, args.groupId!!)
            setTitle(R.string.edit_notification)
        } else {
            setTitle(R.string.new_notification)
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    when (uiState) {
                        is EditNotificationFragmentUIState.Loading -> {
                            setUIState(UIState.LOADING)
                        }
                        is EditNotificationFragmentUIState.Success -> {
                            val notification = uiState.notification?.notification

                            if (notification != null) {
                                val group = uiState.notification.group
                                binding.notificationTitle.setText(notification.title)
                                if (effectiveGroups != null)
                                    binding.notificationGroup.setSelection(effectiveGroups!!.indexOf(group))
                                binding.notificationGroup.isEnabled = false
                                if (colors != null)
                                    binding.notificationAccent.setSelection(colors!!.indexOf(BoardNotificationColors.getByApiColor(notification.color ?: BoardNotificationColor.BLUE)))
                                binding.notificationText.setText(notification.text)
                            }

                            val effectiveGroups = uiState.effectiveGroups
                            binding.notificationGroup.adapter = ScopeSelectionAdapter(requireContext(), effectiveGroups)

                            val colors = BoardNotificationColors.values()
                            binding.notificationAccent.adapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, colors.map { getString(it.text) })

                            setUIState(UIState.READY)
                        }
                        is EditNotificationFragmentUIState.Failure -> {
                            setUIState(UIState.ERROR)
                        }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.edit_options_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.edit_options_item_save) {
            val title = binding.notificationTitle.text.toString()
            val color = BoardNotificationColors.values()[binding.notificationAccent.selectedItemPosition].apiColor
            val text = binding.notificationText.text.toString()

            if (editMode) {
                viewModel.editNotification(title, text, color, null)
                setUIState(UIState.LOADING)
            } else {
                val selectedGroup = binding.notificationGroup.selectedItem as? IGroup?
                if (selectedGroup != null) {
                    viewModel.addNotification(title, text, color, null, selectedGroup)
                    setUIState(UIState.LOADING)
                }
            }
            return true
        }
        return false
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.notificationAccent.isEnabled = newState == UIState.READY
        if (!editMode)
            binding.notificationGroup.isEnabled = newState == UIState.READY
        binding.notificationText.isEnabled = newState == UIState.READY
        binding.notificationTitle.isEnabled = newState == UIState.READY
    }
}