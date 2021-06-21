package de.deftk.openww.android.auth

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AuthenticatorService: Service() {

    private lateinit var authenticator: WebWeaverAuthenticator

    override fun onCreate() {
        authenticator = WebWeaverAuthenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return authenticator.iBinder
    }
}