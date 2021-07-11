package de.deftk.openww.android.adapter.recycler

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.fragments.ActionModeClickListener

abstract class ActionModeAdapter<T, VH : ActionModeAdapter.ActionModeViewHolder>(
    callback: DiffUtil.ItemCallback<T>,
    protected val clickListener: ActionModeClickListener<VH>
) : ListAdapter<T, VH>(callback) {

    val selectedItems: List<VH> = mutableListOf()

    public override fun getItem(position: Int): T {
        return super.getItem(position)
    }

    fun toggleItemSelection(viewHolder: VH, selected: Boolean? = null) {
        val newState = selected ?: !viewHolder.isSelected()
        viewHolder.setSelected(newState)
        if (newState) {
            (selectedItems as MutableList).add(viewHolder)
        } else {
            (selectedItems as MutableList).remove(viewHolder)
        }
    }

    fun clearSelection() {
        selectedItems.forEach { item ->
            item.setSelected(false)
        }
        (selectedItems as MutableList).clear()
    }

    abstract class ActionModeViewHolder(
        itemView: View,
        private val clickListener: ActionModeClickListener<ActionModeViewHolder>
    ) : RecyclerView.ViewHolder(itemView) {

        abstract fun isSelected(): Boolean
        abstract fun setSelected(selected: Boolean)

        init {
            itemView.setOnClickListener {
                clickListener.onClick(it, this)
            }
            itemView.setOnLongClickListener {
                clickListener.onLongClick(it, this)
                true
            }
        }

    }

}