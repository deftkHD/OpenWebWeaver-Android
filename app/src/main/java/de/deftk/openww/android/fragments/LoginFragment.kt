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
import de.deftk.openww.android.databinding.FragmentLoginBinding
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.LoginViewModel

class LoginFragment : AbstractFragment(false) {

    private val loginViewModel by activityViewModels<LoginViewModel>()
    private val navController by lazy { findNavController() }
    private val args: LoginFragmentArgs by navArgs()

    private var loginActionPerformed = false
    private var tokenLogin = false

    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)

        //TODO fix deprecation just as in LaunchFragment
        val authenticatorResponse: AccountAuthenticatorResponse? = requireActivity().intent?.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        authenticatorResponse?.onRequestContinued()
        val forceRemember = args.onlyAdd || authenticatorResponse != null

        binding.txtEmail.setText(args.login.orEmpty())

        binding.chbStayLoggedIn.isVisible = !forceRemember
        binding.chbStayLoggedIn.isChecked = forceRemember

        loginViewModel.loginToken.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success && loginActionPerformed && binding.chbStayLoggedIn.isChecked) {
                AccountManager.get(requireContext()).addAccountExplicitly(
                    Account(response.value.first, getString(R.string.account_type)),
                    response.value.second,
                    null
                )
            }
        }

        loginViewModel.loginResponse.observe(viewLifecycleOwner) { response ->
            if (loginActionPerformed) {
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
                        navController.popBackStack()
                    } else {
                        navController.navigate(LoginFragmentDirections.actionLoginFragmentToOverviewFragment())
                    }
                } else if (response is Response.Failure) {
                    setUIState(UIState.READY) // don't set it to error so the user can try again
                    response.exception.printStackTrace()
                    loginActionPerformed = false
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

        binding.switchLoginMethod.setOnClickListener {
            tokenLogin = !tokenLogin
            setUIState(UIState.READY)
        }

        binding.btnLogin.setOnClickListener {
            if (currentUIState == UIState.READY) {
                loginActionPerformed = true
                setUIState(UIState.LOADING)
                if (tokenLogin) {
                    val username = binding.txtEmail.text.toString()
                    val token = binding.txtPassKey.text.toString()
                    loginViewModel.loginToken(username, token)
                } else {
                    val username = binding.txtEmail.text.toString()
                    val password = binding.txtPassKey.text.toString()
                    val generateToken = binding.chbStayLoggedIn.isChecked
                    if (generateToken) {
                        loginViewModel.loginPasswordCreateToken(username, password)
                    } else {
                        loginViewModel.loginPassword(username, password)
                    }
                }
            }
        }

        return binding.root
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.btnLogin.isEnabled = newState == UIState.READY
        binding.chbStayLoggedIn.isEnabled = newState == UIState.READY
        binding.txtEmail.isEnabled = newState == UIState.READY
        binding.txtPassKey.isEnabled = newState == UIState.READY
        binding.switchLoginMethod.isEnabled = !newState.refreshing

        if (tokenLogin) {
            binding.chbStayLoggedIn.setText(R.string.remember_token)
            binding.txtPassKey.setHint(R.string.token)
            binding.switchLoginMethod.setText(R.string.password_login_link)
        } else {
            binding.chbStayLoggedIn.setText(R.string.stay_logged_in)
            binding.txtPassKey.setHint(R.string.password)
            binding.switchLoginMethod.setText(R.string.token_login_link)
        }
    }
}