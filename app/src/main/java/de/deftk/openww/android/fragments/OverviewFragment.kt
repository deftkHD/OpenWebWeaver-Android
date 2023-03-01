package de.deftk.openww.android.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.OverviewAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentOverviewBinding
import de.deftk.openww.android.feature.AppFeature
import de.deftk.openww.android.feature.overview.AbstractOverviewElement
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.UserViewModel

class OverviewFragment: ContextualFragment(true) {

    companion object {
        private const val LOG_TAG = "OverviewFragment"
    }

    private val userViewModel: UserViewModel by activityViewModels()
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    private lateinit var binding: FragmentOverviewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, bundle: Bundle?): View {
        binding = FragmentOverviewBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        binding.overviewSwipeRefresh.isEnabled = false // will be enabled when apiContext exists
        binding.overviewSwipeRefresh.setOnRefreshListener {
            binding.overviewList.adapter = null
            loginViewModel.apiContext.value?.also { apiContext ->
                userViewModel.loadOverview(getOverviewFeatures(), apiContext)
                setUIState(UIState.LOADING)
            }
        }
        binding.overviewList.setOnItemClickListener { _, _, position, _ ->
            val item = binding.overviewList.getItemAtPosition(position) as AbstractOverviewElement
            val feature = AppFeature.getByOverviewClass(item::class.java)
            if (feature != null)
                navController.navigate(feature.fragmentId)
        }

        userViewModel.overviewResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                binding.overviewList.adapter = OverviewAdapter(requireContext(), response.value)
                Log.i(LOG_TAG, "Initialized ${response.value.size} overview elements")
                setUIState(UIState.READY)

            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_overview_request_failed, response.exception, requireContext())
                setUIState(UIState.ERROR)
            }
        }
        loginViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            binding.overviewSwipeRefresh.isEnabled = apiContext != null
            if (apiContext != null) {
                if (userViewModel.overviewResponse.value == null) {
                    userViewModel.loadOverview(getOverviewFeatures(), apiContext)
                    setUIState(UIState.LOADING)
                }
            } else {
                binding.overviewList.adapter = null
                setUIState(UIState.DISABLED)
            }
        }
        return binding.root
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.overviewSwipeRefresh.isRefreshing = newState.refreshing
        binding.overviewSwipeRefresh.isEnabled = newState.swipeRefreshEnabled
        binding.overviewList.isEnabled = newState.listEnabled
    }

    private fun getOverviewFeatures(): List<AppFeature> {
        return AppFeature.values().filter { it.overviewBuilder != null && (it.preferenceName == null || preferences.getBoolean(it.preferenceName, false)) }
    }

}