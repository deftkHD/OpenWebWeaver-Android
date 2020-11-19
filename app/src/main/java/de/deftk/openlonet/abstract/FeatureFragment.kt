package de.deftk.openlonet.abstract

import de.deftk.openlonet.feature.AppFeature

abstract class FeatureFragment(val feature: AppFeature): StartFragment() {

    override fun getTitle(): String {
        return getString(feature.translationResource)
    }
}