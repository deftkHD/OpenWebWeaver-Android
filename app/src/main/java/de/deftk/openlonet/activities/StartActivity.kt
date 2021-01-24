package de.deftk.openlonet.activities

import android.accounts.Account
import android.accounts.AccountManager
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.Permission
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.abstract.IBackHandler
import de.deftk.openlonet.abstract.IFragmentHandler
import de.deftk.openlonet.abstract.menu.*
import de.deftk.openlonet.abstract.menu.start.FeatureMenuItem
import de.deftk.openlonet.databinding.ActivityStartBinding
import de.deftk.openlonet.feature.AppFeature
import de.deftk.openlonet.fragments.OverviewFragment
import de.deftk.openlonet.fragments.SettingsFragment
import de.deftk.openlonet.fragments.dialog.ChooseAccountDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StartActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, IFragmentHandler {

    companion object {
        const val EXTRA_FOCUS_FEATURE = "de.deftk.openlonet.start.extra_focus_feature"
        const val EXTRA_FOCUS_FEATURE_ARGUMENTS = "de.deftk.openlonet.start.extra_focus_feature_arguments"

        private const val LOG_TAG = "StartActivity"
    }

    private lateinit var binding: ActivityStartBinding

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val menuMap = mutableMapOf<Int, IMenuItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        Log.i(LOG_TAG, "Setting up navigation view")
        binding.navView.getHeaderView(0).findViewById<TextView>(R.id.header_name).text = AuthStore.getApiUser().getFullName()
        binding.navView.getHeaderView(0).findViewById<TextView>(R.id.header_login).text = AuthStore.getApiUser().login
        binding.navView.setNavigationItemSelectedListener(this)
        addMenuItem(object : AbstractNavigableMenuItem(R.string.overview, R.id.main_group, R.drawable.ic_list_24) {
            override fun onClick(activity: AppCompatActivity) {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, OverviewFragment()).commit()
                supportActionBar?.title = getString(R.string.overview)
                binding.navView.menu.getItem(0).isChecked = true
            }
        })
        getAllFeatures().forEach { apiFeature ->
            val appFeature = AppFeature.getByAPIFeature(apiFeature)
            if (appFeature != null)
                addMenuItem(FeatureMenuItem(appFeature))
        }
        addMenuItem(object : AbstractClickableMenuItem(R.string.open_website, R.id.utility_group, R.drawable.ic_language_24) {
            override fun onClick(activity: AppCompatActivity) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val uri = Uri.parse(AuthStore.getApiUser().getAutoLoginUrl(context = AuthStore.getUserContext()))
                        if (preferences.getBoolean("open_link_external", false)) {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = uri
                            withContext(Dispatchers.Main) {
                                startActivity(intent)
                            }
                        } else {
                            CustomTabsIntent.Builder().build().launchUrl(this@StartActivity, uri)
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
        })
        addMenuItem(object : AbstractNavigableMenuItem(R.string.settings, R.id.utility_group, R.drawable.ic_settings_24) {
            override fun onClick(activity: AppCompatActivity) {
                displayFragment(SettingsFragment())
            }
        })
        addMenuItem(object : AbstractClickableMenuItem(R.string.add_account, R.id.account_group, R.drawable.ic_person_add_24) {
            override fun onClick(activity: AppCompatActivity) {
                val intent = Intent(this@StartActivity, LoginActivity::class.java)
                intent.putExtra(LoginActivity.EXTRA_REMEMBER, true)
                finish()
                startActivity(intent)
            }
        })
        val accountManager = AccountManager.get(this@StartActivity)
        val accounts = accountManager.getAccountsByType(AuthStore.ACCOUNT_TYPE)
        if (accounts.size > 1) {
            addMenuItem(object : AbstractClickableMenuItem(R.string.switch_account, R.id.account_group, R.drawable.ic_account_circle_24) {
                override fun onClick(activity: AppCompatActivity) {
                    ChooseAccountDialogFragment(accounts, object: ChooseAccountDialogFragment.AccountSelectListener {
                        override fun onAccountSelected(account: Account) {
                            CoroutineScope(Dispatchers.IO).launch {
                                if (AuthStore.performLogin(account, accountManager, true,this@StartActivity)) {
                                    finish()
                                    startActivity(intent)
                                }
                            }
                        }
                    }).show(supportFragmentManager, "ChooseAccount")
                }
            })
        }
        addMenuItem(object : AbstractNavigableMenuItem(R.string.logout, R.id.account_group, R.drawable.ic_lock_open_24) {
            override fun onClick(activity: AppCompatActivity) {
                logout()
            }
        })

        val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.open_navigation_drawer, R.string.close_navigation_drawer)
        binding.drawerLayout.addDrawerListener(toggle)
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
        if (intent.hasExtra(MainActivity.EXTRA_LOGOUT)) {
            logout()
        } else {
            binding.navView.menu.getItem(0).isChecked = true
        }
        Log.i(LOG_TAG, "Activity created")
    }

    private fun addMenuItem(baseItem: IMenuItem) {
        val menu = binding.navView.menu
        val id = menu.size()
        val item = menu.add(baseItem.getGroup(), id, id, baseItem.getName())
        val isCheckable = baseItem is IMenuNavigable
        item.isCheckable = isCheckable
        item.setIcon(baseItem.getIcon())
        menuMap[id] = baseItem
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
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
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun displayFeatureFragment(appFeature: AppFeature) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, appFeature.fragmentClass.newInstance()).commit()
        supportActionBar?.title = getString(appFeature.translationResource)
        val itemId = menuMap.filterValues { it is FeatureMenuItem }.filter { (it.value as FeatureMenuItem).feature == appFeature }.keys.first()
        binding.navView.menu.getItem(itemId).isChecked = true
    }

    override fun displayFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }

    private fun getAllFeatures(): List<Feature> {
        val features = mutableListOf<Feature>()
        AuthStore.getApiUser().effectiveRights.forEach { permission ->
            Feature.getAvailableFeatures(permission).filter { !features.contains(it) }.forEach { features.add(it) }
        }
        AuthStore.getApiUser().getGroups().forEach { membership ->
            membership.effectiveRights.forEach { permission ->
                Feature.getAvailableFeatures(permission).filter { !features.contains(it) }.forEach { features.add(it) }
            }
        }
        features.addAll(Feature.getAvailableFeatures(Permission.SELF))
        return features
    }

    private fun logout() {
        if (AuthStore.currentApiToken != null) {
            val listener = DialogInterface.OnClickListener { _, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            logout(true)
                        }
                    }
                    DialogInterface.BUTTON_NEGATIVE -> { /* do nothing */ }
                }
            }
            AlertDialog.Builder(this@StartActivity).setMessage(R.string.logout_description).setPositiveButton(R.string.yes, listener).setNegativeButton(R.string.no, listener).show()
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                logout(false)
            }
        }
    }

    private suspend fun logout(removeTrust: Boolean) {
        try {
            if (removeTrust) {
                AuthStore.getApiUser().logoutDestroyToken(AuthStore.currentApiToken!!, AuthStore.getUserContext())
            } else {
                AuthStore.getApiUser().logout(AuthStore.getUserContext())
            }
            withContext(Dispatchers.Main) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    AccountManager.get(this@StartActivity).removeAccountExplicitly(AuthStore.currentAccount)
                } else {
                    @Suppress("DEPRECATION")
                    AccountManager.get(this@StartActivity).removeAccount(AuthStore.currentAccount, null, null)
                }
                val intent = Intent(this@StartActivity, MainActivity::class.java)
                this@StartActivity.startActivity(intent)
                this@StartActivity.finish()
            }
        } catch (e: Exception) {
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
