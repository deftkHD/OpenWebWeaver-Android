package de.deftk.lonet.mobile.feature.overview

interface OverviewCreator {

    fun createOverview(overwriteCache: Boolean): AbstractOverviewElement

}