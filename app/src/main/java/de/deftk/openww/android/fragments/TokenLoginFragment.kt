package de.deftk.openww.android.fragments

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentTokenLoginBinding
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.LoginViewModel


class TokenLoginFragment : AbstractFragment(false) {

    private val loginViewModel by activityViewModels<LoginViewModel>()
    private val navController by lazy { findNavController() }
    private val args: TokenLoginFragmentArgs by navArgs()

    private var actionPerformed = false

    private lateinit var binding: FragmentTokenLoginBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTokenLoginBinding.inflate(inflater, container, false)

        //TODO fix deprecation just as in LaunchFragment
        val authenticatorResponse: AccountAuthenticatorResponse? = requireActivity().intent?.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        authenticatorResponse?.onRequestContinued()
        val forceRemember = args.onlyAdd || authenticatorResponse != null

        binding.txtEmail.setText(args.login.orEmpty())

        binding.chbRememberToken.isVisible = !forceRemember
        binding.chbRememberToken.isChecked = forceRemember

        loginViewModel.loginToken.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success && actionPerformed && binding.chbRememberToken.isChecked) {
                AccountManager.get(requireContext()).addAccountExplicitly(
                    Account(response.value.first, getString(R.string.account_type)),
                    response.value.second,
                    null
                )
            }
        }

        loginViewModel.loginResponse.observe(viewLifecycleOwner) { response ->
            if (actionPerformed) {
                if (response is Response.Success) {
                    setUIState(UIState.READY)
                    if (authenticatorResponse != null) {
                        val bundle = Bundle()
                        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, response.value.user.login)
                        bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type))
                        authenticatorResponse.onResult(bundle)
                        requireActivity().finish()
                        return@observe
                    }

                    if (args.onlyAdd) {
                        navController.popBackStack(R.id.loginFragment, true)
                    } else {
                        navController.navigate(LoginFragmentDirections.actionLoginFragmentToOverviewFragment())
                    }
                } else if (response is Response.Failure) {
                    setUIState(UIState.READY)
                    response.exception.printStackTrace()
                    actionPerformed = false
                    if (authenticatorResponse != null) {
                        val bundle = Bundle()
                        bundle.putString(AccountManager.KEY_ERROR_MESSAGE, response.exception.message ?: response.exception.toString())
                        authenticatorResponse.onResult(bundle)
                        requireActivity().finish()
                        return@observe
                    }
                    Reporter.reportException(R.string.error_login_failed, response.exception, requireContext())
                }
            }
        }

        binding.passwordLogin.setOnClickListener {
            navController.navigate(TokenLoginFragmentDirections.actionTokenLoginFragmentToLoginFragment(args.onlyAdd, binding.txtEmail.text.toString().ifEmpty { null }))
        }

        binding.btnLogin.setOnClickListener {
            if (currentUIState == UIState.READY) {
                actionPerformed = true
                setUIState(UIState.LOADING)
                val username = binding.txtEmail.text.toString()
                val token = binding.txtToken.text.toString()
                loginViewModel.loginToken(username, token)
            }
        }

        return binding.root
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.btnLogin.isEnabled = newState == UIState.READY
        binding.chbRememberToken.isEnabled = newState == UIState.READY
        binding.txtEmail.isEnabled = newState == UIState.READY
        binding.txtToken.isEnabled = newState == UIState.READY
        binding.passwordLogin.isEnabled = !newState.refreshing
    }
}