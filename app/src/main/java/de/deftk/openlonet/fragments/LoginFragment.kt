package de.deftk.openlonet.fragments

import android.accounts.Account
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
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.auth.LoNetAuthenticator
import de.deftk.openlonet.databinding.FragmentLoginBinding
import de.deftk.openlonet.viewmodel.UserViewModel

class LoginFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        userViewModel.loginToken.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                AccountManager.get(requireContext()).addAccountExplicitly(
                    Account(response.value.first, LoNetAuthenticator.ACCOUNT_TYPE),
                    response.value.second,
                    null
                )
            }
        }

        userViewModel.loginResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                navController.navigate(LoginFragmentDirections.actionLoginFragmentToOverviewFragment())
            } else if (response is Response.Failure) {
                response.exception.printStackTrace()
                //TODO handle error message
            }
            binding.pgbLogin.isVisible = false
        }

        binding.tokenLogin.setOnClickListener {
            navController.navigate(LoginFragmentDirections.actionLoginFragmentToTokenLoginFragment())
        }

        binding.btnLogin.setOnClickListener {
            if (!binding.pgbLogin.isVisible) {
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
    }

}