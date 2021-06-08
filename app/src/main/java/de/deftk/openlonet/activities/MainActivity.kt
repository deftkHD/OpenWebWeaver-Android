package de.deftk.openlonet.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
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
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.Permission
import de.deftk.openlonet.R
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.databinding.ActivityMainBinding
import de.deftk.openlonet.feature.AppFeature
import de.deftk.openlonet.viewmodel.UserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ViewModelStoreOwner {

    companion object {
        const val EXTRA_LOGOUT = "de.deftk.openlonet.main.logout"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val userViewModel: UserViewModel by viewModels()
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

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
                R.id.drawer_item_logout -> logout()
                else -> return@setNavigationItemSelectedListener false
            }
            item.isChecked = false
            item.isCheckable = false
            true
        }

        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        userViewModel.apiContext.observe(this) { apiContext ->
            if (apiContext != null) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                binding.navView.getHeaderView(0).findViewById<TextView>(R.id.header_name).text = apiContext.getUser().getFullName()
                binding.navView.getHeaderView(0).findViewById<TextView>(R.id.header_login).text = apiContext.getUser().login

                // cleanup old feature items
                binding.navView.menu.removeGroup(R.id.feature_group)

                // add new feature items
                getEnabledFeatures(apiContext).forEach { feature ->
                    val appFeature = AppFeature.getByAPIFeature(feature)
                    if (appFeature != null) {
                        val menu = binding.navView.menu
                        val item = menu.add(R.id.feature_group, appFeature.fragmentId, Menu.NONE, appFeature.translationResource)
                        item.setIcon(appFeature.drawableResource)
                    }
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

        /*
        // beta disclaimer
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (!preferences.getBoolean("beta_disclaimer_shown", false)) {
            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.beta_disclaimer)
                .setMessage("")
                .setPositiveButton(R.string.next) { _, _ ->
                    preferences.edit().putBoolean("beta_disclaimer_shown", true).apply()
                    showPrivacyDisclaimer()
                }
                .setOnCancelListener {
                    finish()
                }
                .create()
            dialog.show()
            val textView = dialog.findViewById<TextView>(android.R.id.message)
            val str = SpannableString(getString(R.string.beta_disclaimer_text))
            Linkify.addLinks(str, Linkify.WEB_URLS)
            textView.text = str
            textView.movementMethod = LinkMovementMethod.getInstance()
        } else {
            showPrivacyDisclaimer()
        }*/
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
        //TODO add argument REMEMBER=true
        navController.navigate(R.id.loginFragment)
    }

    private fun switchAccount() {
        TODO("not yet implemented")
    }

    private fun logout() {
        userViewModel.logout(this)
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
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    /*private fun showPrivacyDisclaimer() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        val country = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            resources.configuration.locales[0].isO3Country
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale.isO3Country
        }

        if (!preferences.getBoolean("privacy_disclaimer_shown", false) && country == "DEU") {
            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.privacy_title)
                .setMessage("")
                .setPositiveButton(R.string.agree) { _, _ ->
                    preferences.edit().putBoolean("privacy_disclaimer_shown", true).apply()
                    doLogin()
                }
                .setNegativeButton(R.string.decline) { _, _ ->
                    finish()
                }
                .setOnCancelListener {
                    finish()
                }
                .create()
            dialog.show()
            val textView = dialog.findViewById<TextView>(android.R.id.message)
            val str = SpannableString(getString(R.string.privacy_short))
            Linkify.addLinks(str, Linkify.WEB_URLS)
            textView.text = str
            textView.movementMethod = LinkMovementMethod.getInstance()
        } else {
            doLogin()
        }
    }

    private fun doLogin() {
        val logoutName = if (intent.hasExtra(EXTRA_LOGOUT))  intent.getStringExtra(EXTRA_LOGOUT) else null
        AuthStore.doLoginProcedure(this, supportFragmentManager,
            allowNewLogin = true,
            allowRefreshLogin = logoutName == null,
            priorisedLogin = logoutName,
            onSuccess = {
                val intent = Intent(this, StartActivity::class.java)
                if (logoutName != null)
                    intent.putExtra(EXTRA_LOGOUT, logoutName)
                withContext(Dispatchers.Main) {
                    startActivity(intent)
                    finish()
                }
            },
            onFailure = {
                if (logoutName != null) {
                    val accountManager = AccountManager.get(this)
                    val accounts = accountManager.getAccountsByType(AuthStore.ACCOUNT_TYPE).filter { it.name == logoutName }
                    accounts.forEach { account ->
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                            AccountManager.get(this@MainActivity).removeAccountExplicitly(account)
                        } else {
                            @Suppress("DEPRECATION")
                            AccountManager.get(this@MainActivity).removeAccount(account, null, null)
                        }
                    }

                    finish()
                }
            })
    }*/

}
