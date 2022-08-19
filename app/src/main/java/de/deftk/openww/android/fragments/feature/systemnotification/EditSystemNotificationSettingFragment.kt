package de.deftk.openww.android.fragments.feature.systemnotification

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.deftk.openww.android.R

class EditSystemNotificationSettingFragment : Fragment() {

    // see UserViewModel (seems like the edit action does not to anything serverside?)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(
            R.layout.fragment_edit_system_notification_setting,
            container,
            false
        )
    }

}