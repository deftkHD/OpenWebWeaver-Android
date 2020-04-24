package de.deftk.lonet.mobile.abstract

import de.deftk.lonet.mobile.feature.AppFeature

abstract class FeatureFragment(val feature: AppFeature): StartFragment() {

    override fun getTitle(): String {
        return getString(feature.translationResource)
    }
}