package de.deftk.openww.android.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import de.deftk.openww.android.activities.MainActivity
import de.deftk.openww.android.utils.ISearchProvider

abstract class AbstractFragment(private val hasActionBar: Boolean) : Fragment(), MenuProvider {

    protected var uiEnabled: Boolean = true
        private set

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (hasActionBar) {
            getMainActivity().supportActionBar?.show()
        } else {
            getMainActivity().supportActionBar?.hide()
        }
        getMainActivity().addMenuProvider(this, viewLifecycleOwner)
        if (this is ISearchProvider)
            getMainActivity().searchProvider = this
    }

    private fun getMainActivity(): MainActivity {
        return (requireActivity() as? MainActivity?) ?: error("Invalid fragment scope")
    }

    @Deprecated("Rename into setUIState()")
    protected fun enableUI(enabled: Boolean) {
        uiEnabled = enabled
        getMainActivity().progressIndicator.isVisible = !enabled
        onUIStateChanged(enabled)
        if (!enabled)
            invalidateOptionsMenu()
    }

    abstract fun onUIStateChanged(enabled: Boolean)

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    protected fun invalidateOptionsMenu() {
        requireActivity().invalidateOptionsMenu()
    }

    override fun onPrepareMenu(menu: Menu) {
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