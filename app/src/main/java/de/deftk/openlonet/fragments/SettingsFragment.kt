package de.deftk.openlonet.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import de.deftk.openlonet.R
import de.deftk.openlonet.activities.StartActivity

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        (activity as StartActivity?)?.supportActionBar?.setTitle(R.string.settings)
        super.onViewStateRestored(savedInstanceState)
    }

}