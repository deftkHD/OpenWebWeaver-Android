package de.deftk.openlonet.fragments

import android.accounts.Account
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
import de.deftk.openlonet.BuildConfig
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.auth.AuthHelper
import de.deftk.openlonet.databinding.FragmentLaunchBinding
import de.deftk.openlonet.viewmodel.UserViewModel

class LaunchFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentLaunchBinding
    private lateinit var authState: AuthHelper.AuthState

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLaunchBinding.inflate(inflater, container, false)
        binding.version.text = BuildConfig.VERSION_NAME
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        userViewModel.apiContext.observe(viewLifecycleOwner, { apiContext ->
            if (apiContext != null) {
                when (authState) {
                    AuthHelper.AuthState.SINGLE -> navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToOverviewFragment())
                    AuthHelper.AuthState.MULTIPLE -> navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToOverviewFragment())
                    else -> { /* ignore */ }
                }
            } else if (userViewModel.logoutResponse.value == null) {
                Log.e("LaunchFragment", "Failed to obtain apiContext (null)")
                //TODO handle error
                requireActivity().finish()
            }
        })


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
                            navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToLoginFragment())
                        }
                    }
                }
                AuthHelper.AuthState.ADD_NEW -> navController.navigate(LaunchFragmentDirections.actionLaunchFragmentToLoginFragment())
            }
        }
    }

    private fun login(account: Account) {
        val token = AccountManager.get(requireContext()).getPassword(account)
        userViewModel.loginResponse.observe(viewLifecycleOwner) { result ->
            if (result is Response.Success) {
                AuthHelper.rememberLogin(account, requireContext())
            } else if (result is Response.Failure) {
                //TODO handle error
                result.exception.printStackTrace()
            }
        }
        userViewModel.loginAccount(account, token)
    }

}