package de.deftk.lonet.mobile.abstract.menu.start

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.menu.IMenuNavigable
import de.deftk.lonet.mobile.feature.AppFeature

class FeatureMenuItem(val feature: AppFeature): IMenuNavigable {

    override fun getName(): Int {
        return feature.translationResource
    }

    override fun getGroup(): Int {
        return R.id.feature_group
    }

    override fun getIcon(): Int {
        return feature.drawableResource
    }

    override fun onClick(activity: AppCompatActivity) {
        displayFragment(activity)
    }

    fun displayFragment(activity: AppCompatActivity, args: Bundle = Bundle()) {
        activity.supportFragmentManager.beginTransaction().replace(R.id.fragment_container, feature.fragmentClass, args).commit()
        activity.supportActionBar?.title = activity.getString(feature.translationResource)
    }
}