package de.deftk.openww.android.fragments.devtools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.FragmentDevToolsBinding
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.LoginViewModel
import kotlinx.coroutines.*

class DevToolsFragment : AbstractFragment(true, false) {

    private val loginViewModel by activityViewModels<LoginViewModel>()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentDevToolsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDevToolsBinding.inflate(inflater, container, false)

        binding.forceInvalidateSession.setOnClickListener {
            loginViewModel.apiContext.value?.also { apiContext ->
                setUIState(UIState.LOADING)
                val job = Job()
                val coroutine = CoroutineScope(Dispatchers.Main + job)
                coroutine.launch(Dispatchers.IO) {
                    try {
                        apiContext.user.logout(apiContext.userContext())
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), R.string.session_invalidated, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Reporter.reportException(R.string.error_session_invalidation_failed, e, requireContext())
                        e.printStackTrace()
                    } finally {
                        withContext(Dispatchers.Main) {
                            setUIState(UIState.READY) // no need to set to error because it can be ignored
                        }
                    }
                }
            }
        }

        binding.showPastRequests.setOnClickListener {
            navController.navigate(DevToolsFragmentDirections.actionDevToolsFragmentToPastRequestsFragment())
        }

        binding.showExceptions.setOnClickListener {
            navController.navigate(DevToolsFragmentDirections.actionDevToolsFragmentToExceptionsFragment())
        }
        binding.showPermissions.setOnClickListener {
            navController.navigate(DevToolsFragmentDirections.actionDevToolsFragmentToPermissionScopesFragment())
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        binding.handleUncaughtExceptions.isChecked = preferences.getBoolean("devtools_uncaught_exception_handling", false)
        binding.handleUncaughtExceptions.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit().putBoolean("devtools_uncaught_exception_handling", isChecked).apply()
            Toast.makeText(requireContext(), R.string.action_requires_restart, Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.showPastRequests.isEnabled = newState == UIState.READY
        binding.forceInvalidateSession.isEnabled = newState == UIState.READY
        binding.showExceptions.isEnabled = newState == UIState.READY
        binding.handleUncaughtExceptions.isEnabled = newState == UIState.READY
        binding.showPermissions.isEnabled = newState == UIState.READY
    }
}