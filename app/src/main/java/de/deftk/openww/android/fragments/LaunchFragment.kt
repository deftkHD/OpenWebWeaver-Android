package de.deftk.openww.android.fragments

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import de.deftk.openww.android.BuildConfig
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.auth.AuthHelper
import de.deftk.openww.android.databinding.FragmentLaunchBinding
import de.deftk.openww.android.fragments.dialog.ChooseAccountDialogFragmentDirections
import de.deftk.openww.android.viewmodel.UserViewModel

class LaunchFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentLaunchBinding
    private lateinit var authState: AuthHelper.AuthState

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLaunchBinding.inflate(inflater, container, false)
        binding.version.text = BuildConfig.VERSION_NAME
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        userViewModel.loginResponse.observe(viewLifecycleOwner, { response ->
            if (response is Response.Success) {
                when (authState) {
                    AuthHelper.AuthState.SINGLE -> navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToOverviewFragment())
                    AuthHelper.AuthState.MULTIPLE -> navController.navigate(R.id.overviewFragment)
                    else -> { /* ignore */ }
                }
            } else if (response is Response.Failure) {
                Log.e("LaunchFragment", "Failed to obtain apiContext")
                response.exception.printStackTrace()
                //TODO handle error
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
                        navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToChooseAccountDialogFragment())
                    } else {
                        val accounts = AuthHelper.findAccounts(prioritized, requireContext())
                        if (accounts.isNotEmpty()) {
                            login(accounts[0])
                        } else {
                            navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToLoginFragment(false, null))
                        }
                    }
                }
                AuthHelper.AuthState.ADD_NEW -> navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToLoginFragment(false, null))
            }
        }
    }

    private fun login(account: Account) {
        val token = AccountManager.get(requireContext()).getPassword(account)
        userViewModel.loginResponse.observe(viewLifecycleOwner) { result ->
            if (result is Response.Success) {
                AuthHelper.rememberLogin(account.name, requireContext())
            } else if (result is Response.Failure) {
                //TODO handle error
                result.exception.printStackTrace()
            }
        }
        userViewModel.loginAccount(account, token)
    }

}