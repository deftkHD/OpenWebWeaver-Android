package de.deftk.openlonet.feature.overview

import android.content.Context
import android.view.View
import android.view.ViewGroup

abstract class AbstractOverviewAdapter {

    abstract fun getView(position: Int, convertView: View?, parent: ViewGroup, context: Context): View

}