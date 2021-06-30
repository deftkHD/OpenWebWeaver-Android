package de.deftk.openww.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import de.deftk.openww.android.feature.contacts.sync.ContactsSyncAdapter

@HiltAndroidApp
class OpenWebWeaver : Application() {

    companion object {
        lateinit var contactsSyncAdapter: ContactsSyncAdapter
            private set
    }

    override fun onCreate() {
        super.onCreate()
        contactsSyncAdapter = ContactsSyncAdapter(applicationContext)
    }

}