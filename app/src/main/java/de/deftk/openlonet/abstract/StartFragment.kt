package de.deftk.openlonet.abstract

import android.os.Bundle
import androidx.fragment.app.Fragment
import de.deftk.openlonet.activities.StartActivity

abstract class StartFragment : Fragment() {

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        (activity as StartActivity?)?.supportActionBar?.title = getTitle()
        super.onViewStateRestored(savedInstanceState)
    }

    abstract fun getTitle(): String

}