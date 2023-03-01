package de.deftk.openww.android.fragments

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.auth.AuthHelper
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.LoginViewModel

abstract class ContextualFragment(hasActionBar: Boolean, requiresLoadingAtStart: Boolean = true) : AbstractFragment(hasActionBar, requiresLoadingAtStart) {

    // the plan
    // check if api context exists and provide it to the fragment as livedata.
    // also make sure this check is called in the onCreateView method.
    // if no api context exists, try to log in in the background using a here defined routine.
    // if this is not possible (too many accounts, no account, ...) show a fragment to solve the
    // situation.
    // each fragment that requires a logged in user should extend this ContextualFragment class.
    // the launch fragment doesn't have to care about login anymore and only about the launch
    // modes.
    // this will make deeplinks possible, because each deeplink destination will be able to
    // make sure there is a valid session (-> api context) existing in the background.
    // oh and also the UserViewModel has to be adjusted (see deprecations and move stuff to
    // LoginViewModel)

    protected val loginViewModel by activityViewModels<LoginViewModel>()
    protected val navController by lazy { findNavController() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        verifyContext()
    }

    protected fun verifyContext() {
        val authState = AuthHelper.estimateAuthState(requireContext())
        if (loginViewModel.apiContext.value == null) {
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
                    navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToLoginFragment(false, null))
                }
            }
        }
    }

    private fun login(account: Account) {
        val token = AccountManager.get(requireContext()).getPassword(account)
        loginViewModel.loginResponse.observe(viewLifecycleOwner) { result ->
            if (result is Response.Success) {
                AuthHelper.rememberLogin(account.name, requireContext())
            } else if (result is Response.Failure) {
                Reporter.reportException(R.string.error_login_failed, result.exception, requireContext())
            }
        }
        loginViewModel.loginAccount(account, token)
    }

}