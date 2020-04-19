package de.deftk.lonet.mobile.tasks

import android.os.AsyncTask
import android.os.Build
import de.deftk.lonet.api.LoNet
import de.deftk.lonet.api.model.User

class LoginTask(private val callback: ILoginCallback): AsyncTask<Any, Void, LoginTask.LoginResult>() {

    override fun doInBackground(vararg params: Any): LoginResult {
        return try {
            val email = params[0].toString()
            val key = params[1].toString()
            when (params[2]) {
                LoginMethod.PASSWORD -> {
                    LoginResult(LoNet.login(email, key), false, null)
                }
                LoginMethod.PASSWORD_CREATE_TRUST -> {
                    LoginResult(LoNet.loginCreateTrust(email, key, "LoNet² Mobile", "${Build.BRAND} ${Build.MODEL}"), true, null)
                }
                LoginMethod.TRUST -> {
                    LoginResult(LoNet.loginToken(email, key), false, null)
                }
                else -> throw IllegalStateException("Unknown login method: ${params[2]}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LoginResult(null, false, e)
        }
    }

    override fun onPostExecute(result: LoginResult) {
        callback.onLoginResult(result)
    }

    enum class LoginMethod {
        PASSWORD,
        PASSWORD_CREATE_TRUST,
        TRUST
    }

    interface ILoginCallback {
        fun onLoginResult(result: LoginResult)
    }

    class LoginResult(val user: User?, val saveKey: Boolean, val exception: Exception?) {

        fun failed(): Boolean {
            return exception != null && user == null
        }

    }
}