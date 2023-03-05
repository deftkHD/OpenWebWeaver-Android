package de.deftk.openww.android.components

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

class ContextMenuRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private lateinit var contextMenuInfo: RecyclerViewContextMenuInfo

    init {
        itemAnimator = DefaultItemAnimator()
    }

    override fun getContextMenuInfo(): ContextMenu.ContextMenuInfo {
        return contextMenuInfo
    }

    override fun showContextMenuForChild(originalView: View): Boolean {
        val position = getChildLayoutPosition(getChildView(originalView))
        if (position >= 0) {
            val id = adapter!!.getItemId(position)
            contextMenuInfo = RecyclerViewContextMenuInfo(position, id)
            return super.showContextMenuForChild(originalView)
        }
        return false
    }

    private fun getChildView(originalView: View): View {
        if (originalView.layoutParams is LayoutParams)
            return originalView
        return getChildView(originalView.parent as View)
    }

    fun highlightItem(position: Int) {
        smoothScrollToPosition(position)
        val vh = findViewHolderForAdapterPosition(position)
        if (vh != null) {
            val animator = ObjectAnimator.ofInt(vh.itemView, "backgroundColor", Color.GRAY, Color.TRANSPARENT)
            animator.duration = 1000
            animator.startDelay = 500
            animator.setEvaluator(ArgbEvaluator())
            animator.start()
        }
    }

    data class RecyclerViewContextMenuInfo(val position: Int, val id: Long) : ContextMenu.ContextMenuInfo

}