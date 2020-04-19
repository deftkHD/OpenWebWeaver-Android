package de.deftk.lonet.mobile.abstract

import androidx.fragment.app.Fragment
import de.deftk.lonet.mobile.feature.AppFeature

abstract class FeatureFragment(val feature: AppFeature): Fragment() {
}