package de.deftk.openww.android.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

abstract class ScopedViewModel(val savedStateHandle: SavedStateHandle): ViewModel() {

    //TODO automatically save all possible properties to savedStateHandle

    private val properties = mutableListOf<ViewModelProperty<*>>()

    /**
     * Registers a new property of the ViewModel in form of LiveData which is used in the UI for representing something
     *
     * @param name: Not necessarily unique name of the property used to identify and reference it
     * //@param persistent: Whether the property should be stored inside the savedStateHandle and as such persist throughout the process death or not
     * @param scoped: Whether the property is bound to a specific user scope and should be reset when the logged in user changes
     * @param initialValue: The initial value of the LiveData
     */
    protected fun <T> registerProperty(name: String, scoped: Boolean, initialValue: T? = null): MutableLiveData<T> {
        val liveData = MutableLiveData<T>(initialValue)
        properties.add(ViewModelProperty(name, false, scoped, liveData, initialValue))
        return liveData
    }

    fun resetScopedData() {
        properties.filter { it.scoped }.forEach { property ->
            property.accessor.value = property.initialValue
        }
    }

}