package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.databinding.ListItemChatMessageBinding
import de.deftk.openww.android.fragments.feature.messenger.AttachmentDownloader
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.messenger.IQuickMessage

class ChatMessageAdapter(private val userViewModel: UserViewModel, private val downloader: AttachmentDownloader, private val navController: NavController, private val scope: IOperatingScope): ListAdapter<IQuickMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatMessageViewHolder(binding, downloader)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        val alignRight = message.from.login == userViewModel.apiContext.value?.user?.login
        (holder as ChatMessageViewHolder).bind(message, alignRight, navController, scope.login)
    }

    public override fun getItem(position: Int): IQuickMessage {
        return super.getItem(position)
    }

    class ChatMessageViewHolder(val binding: ListItemChatMessageBinding, private val downloader: AttachmentDownloader) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setAttachmentClickListener {
                val attachment = binding.message?.file
                if (attachment != null) {
                    downloader.downloadAttachment(attachment, binding.message!!.fileName!!)
                }
            }
        }

        fun bind(message: IQuickMessage, alignRight: Boolean, navController: NavController, currentScope: String) {
            binding.message = message
            binding.alignRight = alignRight
            binding.navController = navController
            binding.currentScope = currentScope
            binding.executePendingBindings()
        }

    }

}

class ChatMessageDiffCallback: DiffUtil.ItemCallback<IQuickMessage>() {

    override fun areItemsTheSame(oldItem: IQuickMessage, newItem: IQuickMessage): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: IQuickMessage, newItem: IQuickMessage): Boolean {
        return oldItem.equals(newItem)
    }
}