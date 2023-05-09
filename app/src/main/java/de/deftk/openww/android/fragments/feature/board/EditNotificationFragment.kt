package de.deftk.openww.android.fragments.feature.board

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.ScopeSelectionAdapter
import de.deftk.openww.android.databinding.FragmentEditNotificationBinding
import de.deftk.openww.android.exception.ObjectNotFoundException
import de.deftk.openww.android.feature.board.BoardNotificationColors
import de.deftk.openww.android.fragments.ContextualFragment
import de.deftk.openww.android.fragments.feature.board.viewmodel.EditNotificationFragmentUIState
import de.deftk.openww.android.fragments.feature.board.viewmodel.EditNotificationViewModel
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.IScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditNotificationFragment : ContextualFragment(true) {

    //TODO implement board type
    //TODO implement kill date

    private val args: EditNotificationFragmentArgs by navArgs()
    private val viewModel by viewModels<EditNotificationViewModel>()

    private lateinit var binding: FragmentEditNotificationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditNotificationBinding.inflate(inflater, container, false)

        if (args.notificationId != null && args.groupId != null) {
            setTitle(R.string.edit_notification)
        } else {
            setTitle(R.string.new_notification)
        }

        binding.notificationTitle.addTextChangedListener { text ->
            viewModel.title = text?.toString() ?: ""
        }

        binding.notificationText.addTextChangedListener { text ->
            viewModel.text = text?.toString() ?: ""
        }

        binding.notificationAccent.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.color = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.notificationGroup.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.groupId = (binding.notificationGroup.adapter.getItem(position) as IScope).login
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { uiState ->
                        when (uiState) {
                            is EditNotificationFragmentUIState.Loading -> {
                                setUIState(UIState.LOADING)
                            }
                            is EditNotificationFragmentUIState.Success -> {
                                val notification = uiState.notification?.notification

                                val effectiveGroups = uiState.effectiveGroups
                                binding.notificationGroup.adapter = ScopeSelectionAdapter(requireContext(), effectiveGroups)
                                binding.notificationGroup.isEnabled = notification == null
                                binding.notificationGroup.setSelection(effectiveGroups.indexOfFirst { it.login == viewModel.groupId })

                                val colors = BoardNotificationColors.values()
                                binding.notificationAccent.adapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, colors.map { getString(it.text) })
                                binding.notificationAccent.setSelection(viewModel.color)

                                binding.notificationTitle.setText(viewModel.title)
                                binding.notificationText.setText(viewModel.text)

                                setUIState(UIState.READY)
                            }
                            is EditNotificationFragmentUIState.Failure -> {
                                setUIState(UIState.ERROR)
                                if (uiState.throwable is ObjectNotFoundException) {
                                    Reporter.reportException(R.string.error_notification_not_found, viewModel.notificationId.toString(), requireContext())
                                    navController.popBackStack()
                                } else {
                                    Reporter.reportException(R.string.error_other, uiState.throwable, requireContext())
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                }
                launch {
                    viewModel.eventChannelFlow.collect { event ->
                        when (event) {
                            EditNotificationViewModel.EditNotificationEvent.InvalidGroup -> {
                                setUIState(UIState.READY)
                                Reporter.reportException(R.string.error_invalid_scope, "", requireContext())
                            }
                            else -> {
                                setUIState(UIState.READY)
                                navController.popBackStack()
                            }
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
            val selectedGroup = binding.notificationGroup.selectedItem as? IGroup?
            viewModel.submitAction(title, text, color, null, selectedGroup)
            setUIState(UIState.LOADING)
            return true
        }
        return false
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.notificationAccent.isEnabled = newState == UIState.READY
        binding.notificationText.isEnabled = newState == UIState.READY
        binding.notificationTitle.isEnabled = newState == UIState.READY
    }
}