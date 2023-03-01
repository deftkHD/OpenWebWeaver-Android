package de.deftk.openww.android.fragments.feature.systemnotification

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.SystemNotificationSettingAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentSystemNotificationSettingsBinding
import de.deftk.openww.android.fragments.ContextualFragment
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.UserViewModel

class SystemNotificationSettingsFragment : ContextualFragment(true) {

    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var binding: FragmentSystemNotificationSettingsBinding
    private lateinit var adapter: SystemNotificationSettingAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSystemNotificationSettingsBinding.inflate(inflater, container, false)

        adapter = SystemNotificationSettingAdapter()
        binding.systemNotificationSettingList.adapter = adapter
        userViewModel.systemNotificationSettingsResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value)
                binding.systemNotificationSettingsEmpty.isVisible = response.value.isEmpty()
                setUIState(if (response.value.isEmpty()) UIState.EMPTY else UIState.READY)
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                binding.systemNotificationSettingsEmpty.isVisible = false
                Reporter.reportException(R.string.error_get_system_notification_settings, response.exception, requireContext())
            }
            binding.systemNotificationSettingsSwipeRefresh.isRefreshing = false
        }
        binding.systemNotificationSettingList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.systemNotificationSettingsSwipeRefresh.setOnRefreshListener {
            loginViewModel.apiContext.value?.also { apiContext ->
                userViewModel.loadSystemNotificationSettings(apiContext)
            }
        }

        loginViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (userViewModel.systemNotificationSettingsResponse.value == null) {
                    userViewModel.loadSystemNotificationSettings(apiContext)
                    setUIState(UIState.LOADING)
                }
            } else {
                binding.systemNotificationSettingsEmpty.isVisible = false
                adapter.submitList(emptyList())
                setUIState(UIState.DISABLED)
            }
        }

        registerForContextMenu(binding.systemNotificationSettingList)
        return binding.root
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        requireActivity().menuInflater.inflate(R.menu.system_notification_setting_context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.systemNotificationSettingList.adapter as SystemNotificationSettingAdapter
        when (item.itemId) {
            R.id.system_notification_setting_enable_all -> {
                val notificationSetting = adapter.getItem(menuInfo.position)
                val apiContext = loginViewModel.apiContext.value ?: return false
                userViewModel.enableAllSystemNotificationSettingFacilities(notificationSetting, apiContext)
                setUIState(UIState.LOADING)
            }
            R.id.system_notification_setting_disable_all -> {
                val notificationSetting = adapter.getItem(menuInfo.position)
                val apiContext = loginViewModel.apiContext.value ?: return false
                userViewModel.disableAllSystemNotificationSettingFacilities(notificationSetting, apiContext)
                setUIState(UIState.LOADING)
            }
            else -> return false
        }
        return true
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.systemNotificationSettingsSwipeRefresh.isEnabled = newState.swipeRefreshEnabled
        binding.systemNotificationSettingsSwipeRefresh.isRefreshing = newState.refreshing
        binding.systemNotificationSettingList.isEnabled = newState.listEnabled
        binding.systemNotificationSettingsEmpty.isVisible = newState.showEmptyIndicator
    }
}