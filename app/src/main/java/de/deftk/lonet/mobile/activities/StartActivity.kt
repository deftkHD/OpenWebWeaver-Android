package de.deftk.lonet.mobile.activities

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import de.deftk.lonet.api.LoNet
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.IBackHandler
import de.deftk.lonet.mobile.feature.AppFeature
import de.deftk.lonet.mobile.fragments.OverviewFragment
import de.deftk.lonet.mobile.utils.LoggingRequestHandler
import kotlinx.android.synthetic.main.activity_start.*

class StartActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val drawer by lazy { findViewById<DrawerLayout>(R.id.drawer_layout) }
    private val menuMap = mutableMapOf<Int, Feature>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        LoNet.requestHandler = LoggingRequestHandler()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.getHeaderView(0).findViewById<TextView>(R.id.header_name).text = AuthStore.appUser.fullName ?: getString(R.string.unknown_name)
        navigationView.getHeaderView(0).findViewById<TextView>(R.id.header_login).text = AuthStore.appUser.getLogin()
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.menu.add(R.id.main_group, 0, 0, R.string.overview).apply {
            isCheckable = true
            setIcon(R.drawable.ic_overview)
        }
        getAllFeatures().withIndex().forEach { (index, apiFeature) ->
            val featureImpl = AppFeature.getByAPIFeature(apiFeature)
            if (featureImpl != null) {
                val item = navigationView.menu.add(R.id.feature_group, index + 1, featureImpl.ordinal + 1, featureImpl.translationResource)
                item.isCheckable = true
                item.setIcon(featureImpl.drawableResource)
                menuMap[index] = apiFeature
            }
        }
        navigationView.menu.add(R.id.utility_group, navigationView.menu.size(), navigationView.menu.size(), R.string.open_website).apply {
            setIcon(R.drawable.ic_open_website)
        }
        navigationView.menu.add(R.id.utility_group, navigationView.menu.size(), navigationView.menu.size(), R.string.settings).apply {
            setIcon(R.drawable.ic_settings)
        }
        navigationView.menu.add(R.id.utility_group, navigationView.menu.size(), navigationView.menu.size(), R.string.logout).apply {
            setIcon(R.drawable.ic_logout)
        }

        val toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.open_navigation_drawer, R.string.close_navigation_drawer)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        if (savedInstanceState == null) {
            displayOverviewFragment()
        }
        navigationView.menu.getItem(0).isChecked = true
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
            displayOverviewFragment()
            return
        }
        super.onBackPressed()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.order) { //TODO own abstract menu system -> fix supportActionBar title is reset when rotating
            0 -> {
                displayOverviewFragment()
            }
            nav_view.menu.size() - 1 -> {
                val listener = DialogInterface.OnClickListener { _, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            val preferences = getSharedPreferences(AuthStore.PREFERENCE_NAME, 0)
                            LogoutTask().execute(preferences.contains("token"))
                        }
                        DialogInterface.BUTTON_NEGATIVE -> { /* do nothing */ }
                    }
                }
                AlertDialog.Builder(this).setMessage(R.string.logout_description).setPositiveButton(R.string.yes, listener).setNegativeButton(R.string.no, listener).show()
            }
            nav_view.menu.size() - 2 -> {
                Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show()
            }
            nav_view.menu.size() - 3 -> {
                GenerateAutologinUrlTask().execute()
            }
            else -> {
                val clickedApiFeature = menuMap[item.itemId - 1] ?: return false
                val appFeature = AppFeature.getByAPIFeature(clickedApiFeature) ?: return false
                displayFeatureFragment(appFeature)
            }
        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    fun displayFeatureFragment(appFeature: AppFeature) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, appFeature.fragmentClass.newInstance()).commit()
        supportActionBar?.title = getString(appFeature.translationResource)
        nav_view.menu.getItem(appFeature.ordinal + 1).isChecked = true
    }

    private fun getAllFeatures(): List<Feature> {
        val features = mutableListOf<Feature>()
        AuthStore.appUser.permissions.forEach { permission ->
            Feature.getAvailableFeatures(permission).filter { !features.contains(it) }.forEach { features.add(it) }
        }
        AuthStore.appUser.getContext().getGroups().forEach { membership ->
            membership.memberPermissions.forEach { permission ->
                Feature.getAvailableFeatures(permission).filter { !features.contains(it) }.forEach { features.add(it) }
            }
        }
        return features
    }

    private fun displayOverviewFragment() {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, OverviewFragment()).commit()
        supportActionBar?.title = getString(R.string.overview)
        nav_view.menu.getItem(0).isChecked = true
    }

    private inner class LogoutTask: AsyncTask<Boolean, Void, Boolean>() {
        override fun doInBackground(vararg params: Boolean?): Boolean {
            return try {
                AuthStore.appUser.logout(params[0] == true)
                true
            } catch (e: Exception) {
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
