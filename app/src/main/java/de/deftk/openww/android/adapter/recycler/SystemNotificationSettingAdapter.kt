package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.databinding.ListItemSystemNotificationSettingBinding
import de.deftk.openww.api.model.feature.systemnotification.INotificationSetting

class SystemNotificationSettingAdapter: ListAdapter<INotificationSetting, SystemNotificationSettingAdapter.SystemNotificationSettingViewHolder>(SystemNotificationSettingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SystemNotificationSettingViewHolder {
        val binding = ListItemSystemNotificationSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SystemNotificationSettingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SystemNotificationSettingViewHolder, position: Int) {
        val notificationSetting = getItem(position)
        holder.bind(notificationSetting)
    }

    public override fun getItem(position: Int): INotificationSetting {
        return super.getItem(position)
    }

    class SystemNotificationSettingViewHolder(val binding: ListItemSystemNotificationSettingBinding): RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setMenuClickListener {
                itemView.showContextMenu()
            }
            // see UserViewModel (seems like the edit action does not to anything serverside?)
            /*itemView.setOnClickListener {
                val notificationSetting = binding.notificationSetting ?: return@setOnClickListener
                val action = SystemNotificationSettingsFragmentDirections.actionSystemNotificationSettingsFragmentToEditSystemNotificationSettingFragment(notificationSetting.type, notificationSetting.obj)
                itemView.findNavController().navigate(action)
            }*/
        }

        fun bind(notificationSetting: INotificationSetting) {
            binding.notificationSetting = notificationSetting
            binding.executePendingBindings()
        }

    }

}

class SystemNotificationSettingDiffCallback: DiffUtil.ItemCallback<INotificationSetting>() {

    override fun areItemsTheSame(oldItem: INotificationSetting, newItem: INotificationSetting): Boolean {
        return oldItem.type == newItem.type && oldItem.obj == newItem.obj
    }

    override fun areContentsTheSame(oldItem: INotificationSetting, newItem: INotificationSetting): Boolean {
        return oldItem.equals(newItem)
    }
}