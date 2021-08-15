package de.deftk.openww.android.viewmodel

import androidx.lifecycle.ViewModel

abstract class ScopedViewModel: ViewModel() {

    abstract fun resetScopedData()

}