package de.deftk.openlonet.fragments

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.auth.LoNetAuthenticator
import de.deftk.openlonet.databinding.FragmentLoginBinding
import de.deftk.openlonet.viewmodel.UserViewModel

class LoginFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val navController by lazy { findNavController() }
    private val args: LoginFragmentArgs by navArgs()

    private var actionPerformed = false

    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        val authenticatorResponse: AccountAuthenticatorResponse? = requireActivity().intent?.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        authenticatorResponse?.onRequestContinued()
        val forceRemember = args.onlyAdd || authenticatorResponse != null

        binding.txtEmail.setText(args.login.orEmpty())

        binding.chbStayLoggedIn.isVisible = !forceRemember
        binding.chbStayLoggedIn.isChecked = forceRemember

        userViewModel.loginToken.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success && actionPerformed) {
                AccountManager.get(requireContext()).addAccountExplicitly(
                    Account(response.value.first, LoNetAuthenticator.ACCOUNT_TYPE),
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
                        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, response.value.getUser().login)
                        bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, LoNetAuthenticator.ACCOUNT_TYPE)
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
                    response.exception.printStackTrace()
                    actionPerformed = false
                    if (authenticatorResponse != null) {
                        val bundle = Bundle()
                        bundle.putString(AccountManager.KEY_ERROR_MESSAGE, response.exception.message ?: response.exception.toString())
                        authenticatorResponse.onResult(bundle)
                        requireActivity().finish()
                        return@observe
                    }
                    //TODO handle error message
                }
                binding.pgbLogin.isVisible = false
            }
        }

        binding.tokenLogin.setOnClickListener {
            navController.navigate(LoginFragmentDirections.actionLoginFragmentToTokenLoginFragment(binding.txtEmail.text.toString().ifEmpty { null }))
        }

        binding.btnLogin.setOnClickListener {
            if (!binding.pgbLogin.isVisible) {
                actionPerformed = true
                binding.pgbLogin.isVisible = true
                val username = binding.txtEmail.text.toString()
                val password = binding.txtPassword.text.toString()
                val generateToken = binding.chbStayLoggedIn.isChecked
                if (generateToken) {
                    userViewModel.loginPasswordCreateToken(username, password)
                } else {
                    userViewModel.loginPassword(username, password)
                }
            }
        }

        return binding.root
    }

}