package de.deftk.openlonet.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.databinding.FragmentLoginBinding
import de.deftk.openlonet.viewmodel.UserViewModel

class LoginFragment : Fragment() {

    companion object {
        const val LOGIN_SUCCESSFUL = "LOGIN_SUCCESSFUL"
    }

    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var binding: FragmentLoginBinding
    private lateinit var navController: NavController
    private lateinit var savedStateHandle: SavedStateHandle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        navController = findNavController()
        savedStateHandle = navController.previousBackStackEntry!!.savedStateHandle
        savedStateHandle.set(LOGIN_SUCCESSFUL, false)

        userViewModel.loginResource.observe(viewLifecycleOwner) { resource ->
            if (resource is Response.Success) {
                savedStateHandle.set(LOGIN_SUCCESSFUL, true)
                navController.popBackStack()
                //TODO eventually save token
            } else if (resource is Response.Failure) {
                resource.exception.printStackTrace()
                //TODO handle error message
            }
        }

        binding.btnLogin.setOnClickListener {
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