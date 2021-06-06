package de.deftk.openlonet.fragments

import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.OverviewAdapter
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.auth.AuthHelper
import de.deftk.openlonet.databinding.FragmentOverviewBinding
import de.deftk.openlonet.feature.AppFeature
import de.deftk.openlonet.feature.overview.AbstractOverviewElement
import de.deftk.openlonet.viewmodel.UserViewModel

class OverviewFragment: Fragment() {

    companion object {
        private const val LOG_TAG = "OverviewFragment"
    }

    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var binding: FragmentOverviewBinding
    private lateinit var navController: NavController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navController = findNavController()

        val currentBackStackEntry = navController.currentBackStackEntry!!
        val savedStateHandle = currentBackStackEntry.savedStateHandle
        savedStateHandle.getLiveData<Boolean>(LoginFragment.LOGIN_SUCCESSFUL).observe(currentBackStackEntry) { success ->
            if (!success) {
                requireActivity().finish()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, bundle: Bundle?): View {
        binding = FragmentOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.overviewSwipeRefresh.isEnabled = false // will be enabled when apiContext exists
        binding.overviewSwipeRefresh.setOnRefreshListener {
            binding.overviewList.adapter = null
            if (userViewModel.apiContext.value != null) {
                refreshOverview(userViewModel.apiContext.value!!)
            }
        }
        binding.overviewList.setOnItemClickListener { _, _, position, _ ->
            val item = binding.overviewList.getItemAtPosition(position) as AbstractOverviewElement
            val feature = AppFeature.getByOverviewClass(item::class.java)
            if (feature != null)
                navController.navigate(feature.fragmentId)
        }

        if (userViewModel.apiContext.value == null) {
            when (AuthHelper.estimateAuthState(requireContext())) {
                AuthHelper.AuthState.SINGLE -> {
                    val account = AuthHelper.findAccounts(null, requireContext())[0]
                    val token = AccountManager.get(requireContext()).getPassword(account)
                    userViewModel.loginResource.observe(viewLifecycleOwner) { result ->
                        if (result is Response.Success) {
                            AuthHelper.rememberLogin(account, requireContext())
                        } else if (result is Response.Failure) {
                            result.exception.printStackTrace()
                        }
                    }
                    userViewModel.loginAccount(account, token)
                }
                AuthHelper.AuthState.CHOOSE -> {
                    TODO("not implemented yet")
                }
                AuthHelper.AuthState.ADD_NEW -> {
                    navController.navigate(R.id.loginFragment)
                }
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner, { apiContext ->
            binding.overviewSwipeRefresh.isEnabled = apiContext != null
            if (apiContext != null) {
                refreshOverview(apiContext)
            } else {
                navController.navigate(R.id.loginFragment)
            }
        })

        userViewModel.overviewResponse.observe(viewLifecycleOwner) { resource ->
            if (resource is Response.Success) {
                binding.overviewList.adapter = OverviewAdapter(requireContext(), resource.value)
                Log.i(LOG_TAG, "Initialized ${resource.value.size} overview elements")
                binding.progressOverview.visibility = ProgressBar.GONE
                binding.overviewSwipeRefresh.isRefreshing = false
            }
        }
    }

    private fun refreshOverview(apiContext: ApiContext) {
        userViewModel.overviewResponse.observe(viewLifecycleOwner) { resource ->
            if (resource is Response.Failure) {
                resource.exception.printStackTrace()
                Toast.makeText(
                    context,
                    getString(R.string.overview_request_failed).format(resource.exception.message ?: resource.exception),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        userViewModel.loadOverview(apiContext)
    }

}