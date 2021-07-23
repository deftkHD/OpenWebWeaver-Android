package de.deftk.openww.android.fragments

import android.view.Menu
import android.view.View
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import de.deftk.openww.android.R
import de.deftk.openww.android.activities.MainActivity
import de.deftk.openww.android.adapter.recycler.ActionModeAdapter

abstract class ActionModeFragment<T, VH : ActionModeAdapter.ActionModeViewHolder>(@MenuRes private val actionModeMenuResource: Int) : Fragment(), ActionMode.Callback, ActionModeClickListener<VH> {

    protected val adapter: ActionModeAdapter<T, VH> by lazy { createAdapter() }

    protected var actionMode: ActionMode? = null

    abstract fun createAdapter(): ActionModeAdapter<T, VH>

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        actionMode = mode
        (requireActivity() as? MainActivity?)?.actionMode = actionMode
        requireActivity().menuInflater.inflate(actionModeMenuResource, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.title = resources.getQuantityString(R.plurals.selected_count, adapter.selectedItems.size).format(adapter.selectedItems.size)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        adapter.clearSelection()
        (requireActivity() as? MainActivity?)?.actionMode = null
        actionMode = null
    }

    protected fun startActionMode(viewHolder: VH) {
        if (actionMode == null) {
            actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(this)
            adapter.toggleItemSelection(viewHolder)
            actionMode!!.invalidate()
        }
    }

    override fun onClick(view: View, viewHolder: VH) {
        if (actionMode != null) {
            adapter.toggleItemSelection(viewHolder)
            if (adapter.selectedItems.isNotEmpty()) {
                actionMode!!.invalidate()
            } else {
                actionMode!!.finish()
            }
        } else {
            onItemClick(view, viewHolder)
        }
    }

    abstract fun onItemClick(view: View, viewHolder: VH)

    override fun onLongClick(view: View, viewHolder: VH) {
        startActionMode(viewHolder)

    }
}

interface ActionModeClickListener<VH : ActionModeAdapter.ActionModeViewHolder> {
    fun onClick(view: View, viewHolder: VH)
    fun onLongClick(view: View, viewHolder: VH)
}