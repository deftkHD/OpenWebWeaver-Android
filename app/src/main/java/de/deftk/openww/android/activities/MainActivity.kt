package de.deftk.openww.android.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.Permission
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.auth.AuthHelper
import de.deftk.openww.android.databinding.ActivityMainBinding
import de.deftk.openww.android.feature.AppFeature
import de.deftk.openww.android.feature.LaunchMode
import de.deftk.openww.android.viewmodel.UserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ViewModelStoreOwner {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val userViewModel: UserViewModel by viewModels()
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val launchMode by lazy { LaunchMode.getLaunchMode(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setup navigation
        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration.Builder(R.id.overviewFragment)
            .setOpenableLayout(binding.drawerLayout)
            .build()
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        binding.navView.setNavigationItemSelectedListener { item ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            val controllerHandled = item.onNavDestinationSelected(navController)
            if (controllerHandled)
                return@setNavigationItemSelectedListener true
            when (item.itemId) {
                R.id.drawer_item_open_website -> openWebsite()
                R.id.drawer_item_add_account -> addAccount()
                R.id.drawer_item_switch_account -> switchAccount()
                R.id.drawer_item_logout -> userViewModel.logout(userViewModel.apiContext.value!!.getUser().login, this)
                else -> return@setNavigationItemSelectedListener false
            }
            item.isChecked = false
            item.isCheckable = false
            true
        }

        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        userViewModel.apiContext.observe(this) { apiContext ->
            // allow or disallow switching accounts
            binding.navView.menu.findItem(R.id.drawer_item_switch_account).isVisible = AuthHelper.findAccounts(null, this).size > 1

            if (apiContext != null && launchMode == LaunchMode.DEFAULT) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                binding.navView.getHeaderView(0).findViewById<TextView>(R.id.header_name).text = apiContext.getUser().getFullName()
                binding.navView.getHeaderView(0).findViewById<TextView>(R.id.header_login).text = apiContext.getUser().login

                // show or hide feature items
                val enabledFeatures = getEnabledFeatures(apiContext)
                AppFeature.values().forEach { appFeature ->
                    val item = binding.navView.menu.findItem(appFeature.fragmentId)
                    item?.isVisible = enabledFeatures.contains(appFeature.feature)
                }
            } else {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
        }

        userViewModel.logoutResponse.observe(this) { response ->
            if (response is Response.Success) {
                navController.navigate(R.id.launchFragment)
            } else if (response is Response.Failure) {
                //TODO handle error
                response.exception.printStackTrace()
            }
        }

        //TODO beta disclaimer
        //TODO privacy disclaimer
    }

    private fun openWebsite() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = userViewModel.apiContext.value?.getUser() ?: return@launch
                val uri = Uri.parse(user.getAutoLoginUrl(context = user.getRequestContext(userViewModel.apiContext.value!!)))
                if (preferences.getBoolean("open_link_external", false)) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = uri
                    withContext(Dispatchers.Main) {
                        startActivity(intent)
                    }
                } else {
                    CustomTabsIntent.Builder().build().launchUrl(this@MainActivity, uri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        baseContext,
                        getString(R.string.request_failed_other).format(e.message ?: e),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun addAccount() {
        navController.navigate(R.id.loginFragment, Bundle().apply { putBoolean("only_add", true) })
    }

    private fun switchAccount() {
        navController.navigate(R.id.chooseAccountDialogFragment)
    }

    private fun getEnabledFeatures(apiContext: ApiContext): List<Feature> {
        val features = mutableListOf<Feature>()
        val user = apiContext.getUser()
        user.effectiveRights.forEach { permission ->
            Feature.getAvailableFeatures(permission).filter { !features.contains(it) }.forEach { features.add(it) }
        }
        user.getGroups().forEach { group ->
            group.effectiveRights.forEach { permission ->
                Feature.getAvailableFeatures(permission).filter { !features.contains(it) }.forEach { features.add(it) }
            }
        }
        features.addAll(Feature.getAvailableFeatures(Permission.SELF))
        return features
    }

    override fun onSupportNavigateUp(): Boolean {
        if (launchMode == LaunchMode.DEFAULT) {
            return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        } else if (launchMode == LaunchMode.EMAIL) {
            finish()
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (launchMode == LaunchMode.DEFAULT) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                super.onBackPressed()
            }
        } else if (launchMode == LaunchMode.EMAIL) {
            finish()
        }

    }

}
