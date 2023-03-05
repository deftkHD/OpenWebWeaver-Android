package de.deftk.openww.android.fragments

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class LaunchFragment : ContextualFragment(false) {

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    private lateinit var binding: FragmentLaunchBinding

    private val launchMode by lazy { LaunchMode.getLaunchMode(requireActivity().intent) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLaunchBinding.inflate(inflater, container, false)
        getMainActivity().supportActionBar?.hide()
        binding.version.text = BuildConfig.VERSION_NAME
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val authenticatorResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().intent?.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, AccountAuthenticatorResponse::class.java)
        } else {
            @Suppress("DEPRECATION") // already got ya backs
            requireActivity().intent?.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        }

        if (authenticatorResponse != null) {
            authenticatorResponse.onRequestContinued()
            navController.navigate(LaunchFragmentDirections.actionGlobalLoginFragment(true))
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

        super.onViewCreated(view, savedInstanceState) // trigger login procedure

        loginViewModel.loginResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                when (AuthHelper.estimateAuthState(requireContext())) {
                    AuthHelper.AuthState.SINGLE -> {
                        when (launchMode) {
                            LaunchMode.DEFAULT -> navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToOverviewFragment())
                            LaunchMode.EMAIL -> navController.navigate(LaunchFragmentDirections.actionGlobalWriteMailFragment())
                            LaunchMode.FILE_UPLOAD -> navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToFileStorageGraph())
                            else -> { /* ignore */ }
                        }
                    }
                    AuthHelper.AuthState.MULTIPLE -> {
                        when (launchMode) {
                            LaunchMode.DEFAULT -> navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToOverviewFragment())
                            LaunchMode.EMAIL -> navController.navigate(LaunchFragmentDirections.actionGlobalWriteMailFragment())
                            LaunchMode.FILE_UPLOAD -> navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToFileStorageGraph())
                            else -> { /* ignore */ }
                        }
                    }
                    else -> { /* ignore */ }
                }
            } else if (response is Response.Failure) {
                Log.e("LaunchFragment", "Failed to obtain apiContext")
                Reporter.reportException(R.string.error_other, response.exception, requireContext())
                requireActivity().finish()
            }
        }
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {}
}