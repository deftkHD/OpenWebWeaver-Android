package de.deftk.openww.android.viewmodel

import androidx.lifecycle.MutableLiveData

class ViewModelProperty<T>(val name: String, val persistent: Boolean, val scoped: Boolean, val accessor: MutableLiveData<T>, val initialValue: T?)