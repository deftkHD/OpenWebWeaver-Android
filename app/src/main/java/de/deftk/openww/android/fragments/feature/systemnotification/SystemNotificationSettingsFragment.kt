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
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.UserViewModel

class SystemNotificationSettingsFragment : AbstractFragment(true) {

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
            } else if (response is Response.Failure) {
                binding.systemNotificationSettingsEmpty.isVisible = false
                Reporter.reportException(R.string.error_get_system_notification_settings, response.exception, requireContext())
            }
            enableUI(true)
            binding.systemNotificationSettingsSwipeRefresh.isRefreshing = false
        }
        binding.systemNotificationSettingList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.systemNotificationSettingsSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                userViewModel.loadSystemNotificationSettings(apiContext)
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                userViewModel.loadSystemNotificationSettings(apiContext)
                if (userViewModel.systemNotificationSettingsResponse.value == null)
                    enableUI(false)
            } else {
                binding.systemNotificationSettingsEmpty.isVisible = false
                adapter.submitList(emptyList())
                enableUI(false)
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
                val apiContext = userViewModel.apiContext.value ?: return false
                userViewModel.enableAllSystemNotificationSettingFacilities(notificationSetting, apiContext)
                enableUI(false)
            }
            R.id.system_notification_setting_disable_all -> {
                val notificationSetting = adapter.getItem(menuInfo.position)
                val apiContext = userViewModel.apiContext.value ?: return false
                userViewModel.disableAllSystemNotificationSettingFacilities(notificationSetting, apiContext)
                enableUI(false)
            }
            else -> return false
        }
        return true
    }

    override fun onUIStateChanged(enabled: Boolean) {
        binding.systemNotificationSettingsSwipeRefresh.isEnabled = enabled
        binding.systemNotificationSettingList.isEnabled = enabled
    }
}