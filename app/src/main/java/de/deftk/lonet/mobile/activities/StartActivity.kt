package de.deftk.lonet.mobile.activities

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import de.deftk.lonet.api.LoNet
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.IBackHandler
import de.deftk.lonet.mobile.abstract.IFragmentHandler
import de.deftk.lonet.mobile.abstract.menu.AbstractNavigableMenuItem
import de.deftk.lonet.mobile.abstract.menu.IMenuClickable
import de.deftk.lonet.mobile.abstract.menu.IMenuItem
import de.deftk.lonet.mobile.abstract.menu.IMenuNavigable
import de.deftk.lonet.mobile.abstract.menu.start.FeatureMenuItem
import de.deftk.lonet.mobile.feature.AppFeature
import de.deftk.lonet.mobile.fragments.OverviewFragment
import de.deftk.lonet.mobile.tasks.LoginTask
import de.deftk.lonet.mobile.utils.LoggingRequestHandler
import kotlinx.android.synthetic.main.activity_start.*
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

class StartActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, IFragmentHandler, LoginTask.ILoginCallback {

    companion object {
        const val EXTRA_FOCUS_FEATURE = "de.deftk.lonet.mobile.start.extra_focus_feature"
        const val EXTRA_FOCUS_FEATURE_ARGUMENTS = "de.deftk.lonet.mobile.start.extra_focus_feature_arguments"

        private const val LOG_TAG = "StartActivity"
    }

    private val drawer by lazy { findViewById<DrawerLayout>(R.id.drawer_layout) }
    private val menuMap = mutableMapOf<Int, IMenuItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(LOG_TAG, "Creating start activity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        LoNet.requestHandler = LoggingRequestHandler()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        Log.i(LOG_TAG, "Setting up navigation view")
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.getHeaderView(0).findViewById<TextView>(R.id.header_name).text = AuthStore.appUser.fullName ?: getString(R.string.unknown_name)
        navigationView.getHeaderView(0).findViewById<TextView>(R.id.header_login).text = AuthStore.appUser.getLogin()
        navigationView.setNavigationItemSelectedListener(this)
        addMenuItem(object : AbstractNavigableMenuItem(R.string.overview, R.id.main_group, R.drawable.ic_overview) {
            override fun onClick(activity: AppCompatActivity) {
                displayOverviewFragment()
            }
        })
        getAllFeatures().forEach { apiFeature ->
            val appFeature = AppFeature.getByAPIFeature(apiFeature)
            if (appFeature != null)
                addMenuItem(FeatureMenuItem(appFeature))
        }
        addMenuItem(object : AbstractNavigableMenuItem(R.string.open_website, R.id.utility_group, R.drawable.ic_open_website) {
            override fun onClick(activity: AppCompatActivity) {
                GenerateAutologinUrlTask().execute()
            }
        })
        addMenuItem(object : AbstractNavigableMenuItem(R.string.settings, R.id.utility_group, R.drawable.ic_settings) {
            override fun onClick(activity: AppCompatActivity) {
                Toast.makeText(this@StartActivity, "Not implemented yet", Toast.LENGTH_SHORT).show()
            }
        })
        addMenuItem(object : AbstractNavigableMenuItem(R.string.logout, R.id.utility_group, R.drawable.ic_logout) {
            override fun onClick(activity: AppCompatActivity) {
                val listener = DialogInterface.OnClickListener { _, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            val preferences = getSharedPreferences(AuthStore.PREFERENCE_NAME, 0)
                            LogoutTask().execute(preferences.contains("token"))
                        }
                        DialogInterface.BUTTON_NEGATIVE -> { /* do nothing */ }
                    }
                }
                AlertDialog.Builder(this@StartActivity).setMessage(R.string.logout_description).setPositiveButton(R.string.yes, listener).setNegativeButton(R.string.no, listener).show()
            }
        })

        val toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.open_navigation_drawer, R.string.close_navigation_drawer)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        if (savedInstanceState == null) {
            val extraFocusedFeature = intent.getSerializableExtra(EXTRA_FOCUS_FEATURE) as? Feature?
            if (extraFocusedFeature != null) {
                Log.i(LOG_TAG, "Focusing feature $extraFocusedFeature")
                val args = intent.getBundleExtra(EXTRA_FOCUS_FEATURE_ARGUMENTS) ?: Bundle()
                (menuMap.values.filterIsInstance<FeatureMenuItem>().first { it.feature.feature == extraFocusedFeature }).displayFragment(this, args)
            } else {
                Log.i(LOG_TAG, "Displaying overview")
                (menuMap.values.first { it.getName() == R.string.overview } as IMenuNavigable).onClick(this)
            }
        }
        navigationView.menu.getItem(0).isChecked = true
        Log.i(LOG_TAG, "Activity created")
    }

    private fun addMenuItem(baseItem: IMenuItem) {
        val menu = nav_view?.menu ?: return
        val id = menu.size()
        val item = menu.add(baseItem.getGroup(), id, id, baseItem.getName())
        val isCheckable = baseItem is IMenuNavigable
        item.isCheckable = isCheckable
        item.setIcon(baseItem.getIcon())
        menuMap[id] = baseItem
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
            return
        }
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is IBackHandler && currentFragment.onBackPressed())
            return
        if (OverviewFragment::class.java != currentFragment!!::class.java) {
            (menuMap.values.first { it.getName() == R.string.overview } as IMenuNavigable).onClick(this)
            return
        }
        super.onBackPressed()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val baseItem = menuMap[item.itemId] ?: return false
        if (baseItem is IMenuClickable) {
            baseItem.onClick(this)
            item.isChecked = baseItem is IMenuNavigable
        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    fun displayFeatureFragment(appFeature: AppFeature) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, appFeature.fragmentClass.newInstance()).commit()
        supportActionBar?.title = getString(appFeature.translationResource)
        val itemId = menuMap.filterValues { it is FeatureMenuItem }.filter { (it.value as FeatureMenuItem).feature == appFeature }.keys.first()
        nav_view.menu.getItem(itemId).isChecked = true
    }

    override fun displayFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }

    private fun getAllFeatures(): List<Feature> {
        val features = mutableListOf<Feature>()
        AuthStore.appUser.effectiveRights.forEach { permission ->
            Feature.getAvailableFeatures(permission).filter { !features.contains(it) }.forEach { features.add(it) }
        }
        AuthStore.appUser.getContext().getGroups().forEach { membership ->
            membership.effectiveRights.forEach { permission ->
                Feature.getAvailableFeatures(permission).filter { !features.contains(it) }.forEach { features.add(it) }
            }
        }
        return features
    }

    @Deprecated("too simple")
    private fun displayOverviewFragment() {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, OverviewFragment()).commit()
        supportActionBar?.title = getString(R.string.overview)
        nav_view.menu.getItem(0).isChecked = true
    }

    override fun onRestart() {
        super.onRestart()
        VerifySessionTask().execute()
        //TODO VerifySessionTask should also be executed in onCreate() and removed from MainActivity
        
    }

    override fun onLoginResult(result: LoginTask.LoginResult) {
        if (result.failed()) {
            result.exception?.printStackTrace()
            if (result.exception is IOException) {
                when (result.exception) {
                    is UnknownHostException ->
                        Toast.makeText(this, getString(R.string.request_failed_connection), Toast.LENGTH_LONG).show()
                    is TimeoutException ->
                        Toast.makeText(this, getString(R.string.request_failed_timeout), Toast.LENGTH_LONG).show()
                    else ->
                        Toast.makeText(this, String.format(getString(R.string.request_failed_other), result.exception.message), Toast.LENGTH_LONG).show()
                }
            } else {
                getSharedPreferences(AuthStore.PREFERENCE_NAME, 0).edit().remove("token").apply()
                Toast.makeText(this, getString(R.string.token_expired), Toast.LENGTH_LONG).show()

                val intent = Intent(this, LoginActivity::class.java)
                if (AuthStore.getSavedUsername(this) != null) {
                    intent.putExtra(LoginActivity.EXTRA_LOGIN, AuthStore.getSavedUsername(this))
                }
                startActivity(intent)
                finish()
            }
        } else {
            AuthStore.appUser = result.user!!
        }

        //TODO hide progressbar, show fragment
    }

    private inner class VerifySessionTask: AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg params: Void): Boolean {
            //AuthStore.appUser.logout(false) //FIXME JUST FOR TESTING, REMOVE FOR RELEASE!!!
            return AuthStore.appUser.checkSession()
        }

        override fun onPostExecute(result: Boolean) {
            if (!result) { // session expired
                if (AuthStore.getSavedToken(this@StartActivity) == null) {
                    // create new account or simply login without adding an account
                    val intent = Intent(this@StartActivity, LoginActivity::class.java)
                    if (AuthStore.getSavedUsername(this@StartActivity) != null) {
                        intent.putExtra(LoginActivity.EXTRA_LOGIN, AuthStore.getSavedUsername(this@StartActivity))
                    }
                    startActivity(intent)
                    finish()

                } else {
                    // use saved account
                    LoginTask(this@StartActivity).execute(
                        AuthStore.getSavedUsername(this@StartActivity),
                        AuthStore.getSavedToken(this@StartActivity),
                        LoginTask.LoginMethod.TRUST
                    )
                }
            }
        }
    }

    private inner class LogoutTask: AsyncTask<Boolean, Void, Boolean>() {
        override fun doInBackground(vararg params: Boolean?): Boolean {
            return try {
                AuthStore.appUser.logout(params[0] == true)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                this@StartActivity.getSharedPreferences(AuthStore.PREFERENCE_NAME, 0).edit().remove("token").apply()
                this@StartActivity.getSharedPreferences(AuthStore.PREFERENCE_NAME, 0).edit().remove("login").apply()
                val intent = Intent(this@StartActivity, LoginActivity::class.java)
                this@StartActivity.startActivity(intent)
                this@StartActivity.finish()
            } else {
                Toast.makeText(baseContext, getString(R.string.request_failed_other).format("No details"), Toast.LENGTH_LONG).show()
            }
        }
    }

    private inner class GenerateAutologinUrlTask: AsyncTask<Void, Void, String?>() {
        override fun doInBackground(vararg params: Void?): String? {
            return try {
                AuthStore.appUser.getAutoLoginUrl()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        //TODO setting to choose if open inside new browser tap or in single browser window
        override fun onPostExecute(result: String?) {
            if (result != null) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(result)
                startActivity(intent)
            } else {
                Toast.makeText(baseContext, getString(R.string.request_failed_other).format("No details"), Toast.LENGTH_LONG).show()
            }
        }
    }

}
