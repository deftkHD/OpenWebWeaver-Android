package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.databinding.ListItemNoteBinding
import de.deftk.openww.android.fragments.feature.notes.NotesFragmentDirections
import de.deftk.openww.api.model.feature.notes.INote

class NoteAdapter : ListAdapter<INote, RecyclerView.ViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val note = getItem(position)
        (holder as NoteViewHolder).bind(note)
    }

    public override fun getItem(position: Int): INote {
        return super.getItem(position)
    }

    class NoteViewHolder(val binding: ListItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setClickListener {
                itemView.findNavController().navigate(NotesFragmentDirections.actionNotesFragmentToReadNoteFragment(binding.note!!.id))
            }
            itemView.setOnLongClickListener {
                itemView.showContextMenu()
                true
            }
        }

        fun bind(note: INote) {
            binding.note = note
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