package de.deftk.openww.android.fragments

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentTokenLoginBinding
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.UserViewModel


class TokenLoginFragment : AbstractFragment(true) {

    private val userViewModel: UserViewModel by activityViewModels()
    private val navController by lazy { findNavController() }
    private val args: TokenLoginFragmentArgs by navArgs()

    private var actionPerformed = false

    private lateinit var binding: FragmentTokenLoginBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTokenLoginBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        val authenticatorResponse: AccountAuthenticatorResponse? = requireActivity().intent?.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        authenticatorResponse?.onRequestContinued()
        val forceRemember = args.onlyAdd || authenticatorResponse != null

        binding.txtEmail.setText(args.login.orEmpty())

        binding.chbRememberToken.isVisible = !forceRemember
        binding.chbRememberToken.isChecked = forceRemember

        userViewModel.loginToken.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success && actionPerformed && binding.chbRememberToken.isChecked) {
                AccountManager.get(requireContext()).addAccountExplicitly(
                    Account(response.value.first, getString(R.string.account_type)),
                    response.value.second,
                    null
                )
            }
        }

        userViewModel.loginResponse.observe(viewLifecycleOwner) { response ->
            if (actionPerformed) {
                if (response is Response.Success) {
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
                enableUI(true)
            }
        }

        binding.passwordLogin.setOnClickListener {
            navController.navigate(TokenLoginFragmentDirections.actionTokenLoginFragmentToLoginFragment(args.onlyAdd, binding.txtEmail.text.toString().ifEmpty { null }))
        }

        binding.btnLogin.setOnClickListener {
            if (uiEnabled) {
                actionPerformed = true
                enableUI(false)
                val username = binding.txtEmail.text.toString()
                val token = binding.txtToken.text.toString()
                userViewModel.loginToken(username, token)
            }
        }

        return binding.root
    }

    override fun onUIStateChanged(enabled: Boolean) {
        binding.btnLogin.isEnabled = enabled
        binding.chbRememberToken.isEnabled = enabled
        binding.txtEmail.isEnabled = enabled
        binding.txtToken.isEnabled = enabled
    }
}