package de.deftk.openlonet.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.OverviewAdapter
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.databinding.FragmentOverviewBinding
import de.deftk.openlonet.feature.AppFeature
import de.deftk.openlonet.feature.overview.AbstractOverviewElement
import de.deftk.openlonet.viewmodel.UserViewModel

class OverviewFragment: Fragment() {

    //TODO recycler view

    companion object {
        private const val LOG_TAG = "OverviewFragment"
    }

    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var binding: FragmentOverviewBinding
    private lateinit var navController: NavController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navController = findNavController()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, bundle: Bundle?): View {
        binding = FragmentOverviewBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.overviewSwipeRefresh.isEnabled = false // will be enabled when apiContext exists
        binding.overviewSwipeRefresh.setOnRefreshListener {
            binding.overviewList.adapter = null
            userViewModel.apiContext.value?.also { apiContext ->
                userViewModel.loadOverview(apiContext)
            }
        }
        binding.overviewList.setOnItemClickListener { _, _, position, _ ->
            val item = binding.overviewList.getItemAtPosition(position) as AbstractOverviewElement
            val feature = AppFeature.getByOverviewClass(item::class.java)
            if (feature != null)
                navController.navigate(feature.fragmentId)
        }

        userViewModel.overviewResponse.observe(viewLifecycleOwner) { resource ->
            if (resource is Response.Success) {
                binding.overviewList.adapter = OverviewAdapter(requireContext(), resource.value)
                Log.i(LOG_TAG, "Initialized ${resource.value.size} overview elements")
                binding.progressOverview.visibility = ProgressBar.GONE
                binding.overviewSwipeRefresh.isRefreshing = false
            } else if (resource is Response.Failure) {
                //TODO handle error
                resource.exception.printStackTrace()
                Toast.makeText(
                    context,
                    getString(R.string.overview_request_failed).format(resource.exception.message ?: resource.exception),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            binding.overviewSwipeRefresh.isEnabled = apiContext != null
            if (apiContext != null) {
                userViewModel.loadOverview(apiContext)
            } else {
                binding.progressOverview.isVisible = true
                binding.overviewList.adapter = null
            }
        }
    }

}