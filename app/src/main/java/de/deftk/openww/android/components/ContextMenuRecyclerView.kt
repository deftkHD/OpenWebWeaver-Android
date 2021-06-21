package de.deftk.openww.android.components

import android.content.Context
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ContextMenuRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private lateinit var contextMenuInfo: RecyclerViewContextMenuInfo

    override fun getContextMenuInfo(): ContextMenu.ContextMenuInfo {
        return contextMenuInfo
    }

    override fun showContextMenuForChild(originalView: View): Boolean {
        val position = getChildLayoutPosition(originalView)
        if (position >= 0) {
            val id = adapter!!.getItemId(position)
            contextMenuInfo = RecyclerViewContextMenuInfo(position, id)
            return super.showContextMenuForChild(originalView)
        }
        return false
    }

    data class RecyclerViewContextMenuInfo(val position: Int, val id: Long) : ContextMenu.ContextMenuInfo

}