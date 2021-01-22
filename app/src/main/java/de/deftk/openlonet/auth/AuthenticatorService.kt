package de.deftk.openlonet.auth

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AuthenticatorService: Service() {

    private lateinit var authenticator: LoNetAuthenticator

    override fun onCreate() {
        authenticator = LoNetAuthenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return authenticator.iBinder
    }
}