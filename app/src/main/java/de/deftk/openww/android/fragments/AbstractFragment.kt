package de.deftk.openww.android.fragments

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import de.deftk.openww.android.activities.MainActivity
import de.deftk.openww.android.utils.ISearchProvider

abstract class AbstractFragment(private val hasActionBar: Boolean) : Fragment() {

    protected var uiEnabled: Boolean = true
        private set

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (hasActionBar) {
            getMainActivity().supportActionBar?.show()
        } else {
            getMainActivity().supportActionBar?.hide()
        }
        if (this is ISearchProvider)
            getMainActivity().searchProvider = this
    }

    private fun getMainActivity(): MainActivity {
        return (requireActivity() as? MainActivity?) ?: error("Invalid fragment scope")
    }

    protected fun enableUI(enabled: Boolean) {
        uiEnabled = enabled
        getMainActivity().progressIndicator.isVisible = !enabled
        onUIStateChanged(enabled)
        invalidateOptionsMenu()
    }

    abstract fun onUIStateChanged(enabled: Boolean)

    protected fun invalidateOptionsMenu() {
        requireActivity().invalidateOptionsMenu()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.forEach { item ->
            item.isEnabled = uiEnabled
        }
    }

    override fun onDestroy() {
        if (this is ISearchProvider)
            getMainActivity().searchProvider = null
        super.onDestroy()
    }

}