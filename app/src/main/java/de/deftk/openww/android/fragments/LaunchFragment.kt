package de.deftk.openww.android.fragments

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import de.deftk.openww.android.BuildConfig
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.auth.AuthHelper
import de.deftk.openww.android.databinding.FragmentLaunchBinding
import de.deftk.openww.android.feature.LaunchMode
import de.deftk.openww.android.fragments.dialog.BetaDisclaimerFragment
import de.deftk.openww.android.fragments.dialog.PrivacyDialogFragment
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.UserViewModel

class LaunchFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val navController by lazy { findNavController() }
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    private lateinit var binding: FragmentLaunchBinding
    private lateinit var authState: AuthHelper.AuthState

    private val launchMode by lazy { LaunchMode.getLaunchMode(requireActivity().intent) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLaunchBinding.inflate(inflater, container, false)
        binding.version.text = BuildConfig.VERSION_NAME
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        userViewModel.loginResponse.observe(viewLifecycleOwner, { response ->
            if (response is Response.Success) {
                when (authState) {
                    AuthHelper.AuthState.SINGLE -> {
                        if (launchMode == LaunchMode.DEFAULT) {
                            navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToOverviewFragment())
                        } else if (launchMode == LaunchMode.EMAIL) {
                            navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToWriteMailFragment())
                        }
                    }
                    AuthHelper.AuthState.MULTIPLE -> {
                        if (launchMode == LaunchMode.DEFAULT) {
                            navController.navigate(R.id.overviewFragment)
                        } else if (launchMode == LaunchMode.EMAIL) {
                            navController.navigate(R.id.writeMailFragment)
                        }
                    }
                    else -> { /* ignore */ }
                }
            } else if (response is Response.Failure) {
                Log.e("LaunchFragment", "Failed to obtain apiContext")
                Reporter.reportException(R.string.error_other, response.exception, requireContext())
                requireActivity().finish()
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val authenticatorResponse: AccountAuthenticatorResponse? = requireActivity().intent?.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        if (authenticatorResponse != null) {
            authenticatorResponse.onRequestContinued()
            navController.navigate(R.id.loginFragment, Bundle().apply { putBoolean("only_add", true) })
            return
        }

        if (!preferences.getBoolean(BetaDisclaimerFragment.BETA_DISCLAIMER_SHOWN_KEY, false)) {
            navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToBetaDisclaimerFragment())
            return
        }

        if (!preferences.getBoolean(PrivacyDialogFragment.PRIVACY_STATEMENT_SHOWN_KEY, false)) {
            navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToPrivacyDialogFragment())
            return
        }

        authState = AuthHelper.estimateAuthState(requireContext())
        if (userViewModel.apiContext.value == null) {
            when (authState) {
                AuthHelper.AuthState.SINGLE -> {
                    val account = AuthHelper.findAccounts(null, requireContext())[0]
                    login(account)
                }
                AuthHelper.AuthState.MULTIPLE -> {
                    val prioritized = AuthHelper.getRememberedLogin(requireContext())
                    if (prioritized == null) {
                        navController.navigate(R.id.chooseAccountDialogFragment)
                    } else {
                        val accounts = AuthHelper.findAccounts(prioritized, requireContext())
                        login(accounts[0])
                    }
                }
                AuthHelper.AuthState.ADD_NEW -> {
                    if (launchMode == LaunchMode.DEFAULT) {
                        navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToLoginFragment(false, null))
                    } else if (launchMode == LaunchMode.EMAIL) {
                        Toast.makeText(requireContext(), R.string.login_failed, Toast.LENGTH_LONG).show()
                        requireActivity().finish()
                    }

                }
            }
        }
    }

    private fun login(account: Account) {
        val token = AccountManager.get(requireContext()).getPassword(account)
        userViewModel.loginResponse.observe(viewLifecycleOwner) { result ->
            if (result is Response.Success) {
                AuthHelper.rememberLogin(account.name, requireContext())
            } else if (result is Response.Failure) {
                Reporter.reportException(R.string.error_login_failed, result.exception, requireContext())
            }
        }
        userViewModel.loginAccount(account, token)
    }

}