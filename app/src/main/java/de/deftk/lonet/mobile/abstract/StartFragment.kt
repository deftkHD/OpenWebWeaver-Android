package de.deftk.lonet.mobile.abstract

import android.os.Bundle
import androidx.fragment.app.Fragment
import de.deftk.lonet.mobile.activities.StartActivity

abstract class StartFragment : Fragment() {

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        (activity as StartActivity?)?.supportActionBar?.title = getTitle()
        super.onViewStateRestored(savedInstanceState)
    }

    abstract fun getTitle(): String

}