package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import de.deftk.openww.android.databinding.ListItemNoteBinding
import de.deftk.openww.android.fragments.ActionModeClickListener
import de.deftk.openww.api.model.IUser
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.notes.INote

class NoteAdapter(clickListener: ActionModeClickListener<NoteViewHolder>, var user: IUser) : ActionModeAdapter<INote, NoteAdapter.NoteViewHolder>(NoteDiffCallback(), clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ListItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding, clickListener as ActionModeClickListener<ActionModeViewHolder>)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note, user)
    }

    class NoteViewHolder(val binding: ListItemNoteBinding, clickListener: ActionModeClickListener<ActionModeViewHolder>) : ActionModeAdapter.ActionModeViewHolder(binding.root, clickListener) {

        private var selected = false

        init {
            binding.setMenuClickListener {
                itemView.showContextMenu()
            }
        }

        override fun isSelected(): Boolean {
            return selected
        }

        override fun setSelected(selected: Boolean) {
            this.selected = selected
            binding.selected = selected
        }

        fun bind(note: INote, user: IUser) {
            binding.note = note
            binding.moreButton.visibility = if (user.effectiveRights.contains(Permission.NOTES_WRITE) || user.effectiveRights.contains(Permission.NOTES_ADMIN)) View.VISIBLE else View.INVISIBLE
            binding.executePendingBindings()
        }

    }

}

class NoteDiffCallback : DiffUtil.ItemCallback<INote>() {

    override fun areItemsTheSame(oldItem: INote, newItem: INote): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: INote, newItem: INote): Boolean {
        return oldItem.equals(newItem)
    }
}