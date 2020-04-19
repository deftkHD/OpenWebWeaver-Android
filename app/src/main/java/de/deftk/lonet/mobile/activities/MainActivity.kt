package de.deftk.lonet.mobile.activities

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.BuildConfig
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.tasks.LoginTask
import de.deftk.lonet.mobile.update.Updater
import java.io.File

class MainActivity : AppCompatActivity(), LoginTask.ILoginCallback, Updater.IUpdateCallback {

    private val pgbUpdate by lazy { findViewById<ProgressBar>(R.id.pgbUpdate) }

    private val targetFile by lazy { File(cacheDir, "lnm_update_stable.apk") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        // cleanup old updates
        if (targetFile.exists())
            targetFile.delete()

        val lblVersion = findViewById<TextView>(R.id.lblVersion)
        lblVersion.append(" ${BuildConfig.VERSION_NAME}")

        // check if app has write & read permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder(this)
                        .setMessage(R.string.files_permission_description)
                        .setPositiveButton(R.string.confirm) { _, _ ->
                            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                        }
                        .show()
                } else {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                }
            } else {
                Updater(targetFile, this).execute(Updater.MODE_CHECK_VERSION)
            }
        } else {
            Updater(targetFile, this).execute(Updater.MODE_CHECK_VERSION)
        }
    }

    override fun onLoginResult(result: LoginTask.LoginResult) {
        if (result.failed()) {
            getSharedPreferences(AuthStore.PREFERENCE_NAME, 0).edit().remove("token").apply()
            Toast.makeText(this, getString(R.string.token_expired), Toast.LENGTH_LONG).show()

            val intent = Intent(this, LoginActivity::class.java)
            if (AuthStore.getSavedUsername(this) != null) {
                intent.putExtra(LoginActivity.EXTRA_LOGIN, AuthStore.getSavedUsername(this))
            }
            startActivity(intent)
            finish()
        } else {
            AuthStore.appUser = result.user ?: error("How should I understand this?")

            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onVersionCheckResult(onlineVersion: String) {
        val currentVersion = BuildConfig.VERSION_NAME.split(".").map { it.toInt() }
        val onlineVersionParts = onlineVersion.split(".").map { it.toInt() }

        val current = (currentVersion[0] shl 18) or (currentVersion[1] shl 9) or currentVersion[2]
        val online =
            (onlineVersionParts[0] shl 18) or (onlineVersionParts[1] shl 9) or onlineVersionParts[2]

        if (online > current) { // new version available
            val dialogListener = DialogInterface.OnClickListener { _, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        pgbUpdate.visibility = ProgressBar.VISIBLE
                        Updater(targetFile, this).execute(Updater.MODE_DOWNLOAD_UPDATE)
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                        finish()
                    }
                }
            }
            AlertDialog.Builder(this)
                .setMessage(R.string.new_update)
                .setPositiveButton(R.string.yes, dialogListener)
                .setNegativeButton(R.string.later, dialogListener)
                .show()
        } else {
            if (AuthStore.getSavedToken(this) == null) {
                // create new account or simply login without adding an account
                val intent = Intent(this, LoginActivity::class.java)
                if (AuthStore.getSavedUsername(this) != null) {
                    intent.putExtra(LoginActivity.EXTRA_LOGIN, AuthStore.getSavedUsername(this))
                }
                startActivity(intent)
                finish()

            } else {
                // use saved account
                LoginTask(this).execute(
                    AuthStore.getSavedUsername(this),
                    AuthStore.getSavedToken(this),
                    LoginTask.LoginMethod.TRUST
                )
            }
        }
    }

    //TODO functionality not verified yet
    override fun onUpdateDownloaded(file: File) {
        pgbUpdate.visibility = ProgressBar.INVISIBLE

        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            intent.setDataAndType(FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", file), "application/vnd.android.package-archive")
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
        }
        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            0 -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish()
                } else {
                    Updater(targetFile, this).execute(Updater.MODE_CHECK_VERSION)
                }
            }
        }
    }

    override fun onUpdateException(message: String) {
        if (message != Updater.CHECKSUM_FAILED) {
            onVersionCheckResult("0.0.0") // bypass update check
        } else {
            AlertDialog.Builder(this)
                .setMessage("${getString(R.string.update_failed)} (CHECKSUM_FAILED)")
                .setPositiveButton(R.string.try_later) { _, _ ->
                    finish()
                }
                .show()
        }
    }

}
