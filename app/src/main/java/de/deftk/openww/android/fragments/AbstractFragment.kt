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

    protected var currentUIState = UIState.EMPTY
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
        setUIState(UIState.LOADING)
    }

    private fun getMainActivity(): MainActivity {
        return (requireActivity() as? MainActivity?) ?: error("Invalid fragment scope")
    }

    protected fun setUIState(newState: UIState) {
        val oldState = currentUIState
        currentUIState = newState
        getMainActivity().progressIndicator.isVisible = newState == UIState.LOADING
        if (newState == UIState.ERROR || newState == UIState.LOADING)
            invalidateOptionsMenu()
        onUIStateChanged(newState, oldState)
    }

    abstract fun onUIStateChanged(newState: UIState, oldState: UIState)

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    protected fun invalidateOptionsMenu() {
        requireActivity().invalidateOptionsMenu()
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.forEach { item ->
            item.isEnabled = currentUIState == UIState.READY || currentUIState == UIState.EMPTY
        }
    }

    override fun onDestroy() {
        if (this is ISearchProvider)
            getMainActivity().searchProvider = null
        super.onDestroy()
    }

    enum class UIState(val listEnabled: Boolean, val swipeRefreshEnabled: Boolean, val refreshing: Boolean, val showEmptyIndicator: Boolean) {
        LOADING(false, false, true, false), // data is currently calculated or being fetched from network
        READY(true, true, false, false), // loading data has been finished and it's ready to be displayed
        EMPTY(true, true, false, true), // load data has been finished, but there is none
        ERROR(false, true, false, false), // error occurred while loading data
        DISABLED(false, false, false, false) // e.g. context is empty
    }

}