package de.deftk.openww.android.fragments.devtools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.FragmentDevToolsBinding
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.UserViewModel
import kotlinx.coroutines.*

class DevToolsFragment : AbstractFragment(true) {

    //TODO list with recent exceptions (capture inside Reporter class)

    private val userViewModel by activityViewModels<UserViewModel>()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentDevToolsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDevToolsBinding.inflate(inflater, container, false)

        binding.forceInvalidateSession.setOnClickListener {
            userViewModel.apiContext.value?.also { apiContext ->
                enableUI(false)
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
                            enableUI(true)
                        }
                    }
                }
            }
        }

        binding.showPastRequests.setOnClickListener {
            navController.navigate(DevToolsFragmentDirections.actionDevToolsFragmentToPastRequestsFragment())
        }

        return binding.root
    }

    override fun onUIStateChanged(enabled: Boolean) {
        binding.showPastRequests.isEnabled = enabled
        binding.forceInvalidateSession.isEnabled = enabled
    }
}