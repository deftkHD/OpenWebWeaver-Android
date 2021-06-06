package de.deftk.openlonet.adapter.recycler

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.databinding.BindingAdapter
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.lonet.api.model.feature.mailbox.IEmail
import de.deftk.lonet.api.model.feature.mailbox.IEmailFolder
import de.deftk.openlonet.activities.MainActivity
import de.deftk.openlonet.databinding.ListItemMailBinding
import de.deftk.openlonet.fragments.feature.mail.MailFragmentDirections
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.viewmodel.UserViewModel
import java.util.*

class MailAdapter : ListAdapter<Pair<IEmail, IEmailFolder>, RecyclerView.ViewHolder>(EmailDiffCallback()) {

    companion object {

        @JvmStatic
        @BindingAdapter("app:mailAuthor")
        fun mailAuthor(view: TextView, email: IEmail) {
            var author = email.getFrom()?.joinToString(", ") { it.name }
            if (author == null) {
                val context = view.context
                author = if (context is MainActivity) {
                    val userViewModel by context.viewModels<UserViewModel>()
                    userViewModel.apiContext.value?.getUser()?.name ?: "UNKNOWN"
                } else {
                    "UNKNOWN"
                }
            }
            view.text = author
        }

        @JvmStatic
        @BindingAdapter("app:dateText")
        fun dateText(view: TextView, date: Date) {
            view.text = TextUtils.parseShortDate(date)
        }

        @JvmStatic
        @BindingAdapter("app:bold")
        fun bold(view: TextView, bold: Boolean) {
            if (bold) {
                view.setTypeface(null, Typeface.BOLD)
            } else {
                view.setTypeface(null, Typeface.NORMAL)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemMailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EmailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (email, folder) = getItem(position)
        (holder as EmailViewHolder).bind(email, folder)
    }

    public override fun getItem(position: Int): Pair<IEmail, IEmailFolder> {
        return super.getItem(position)
    }

    class EmailViewHolder(val binding: ListItemMailBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setClickListener { view ->
                view.findNavController().navigate(MailFragmentDirections.actionMailFragmentToReadMailFragment(binding.folder!!.id, binding.email!!.id))
            }
            itemView.setOnLongClickListener {
                itemView.showContextMenu()
                true
            }
        }

        fun bind(email: IEmail, folder: IEmailFolder) {
            binding.email = email
            binding.folder = folder
            binding.executePendingBindings()
        }

    }

}

class EmailDiffCallback : DiffUtil.ItemCallback<Pair<IEmail, IEmailFolder>>() {

    override fun areItemsTheSame(oldItem: Pair<IEmail, IEmailFolder>, newItem: Pair<IEmail, IEmailFolder>): Boolean {
        return oldItem.first.id == newItem.first.id && oldItem.second.id == newItem.second.id
    }

    override fun areContentsTheSame(oldItem: Pair<IEmail, IEmailFolder>, newItem: Pair<IEmail, IEmailFolder>): Boolean {
        return oldItem.first.equals(newItem.first) && oldItem.second.id == newItem.second.id
    }
}