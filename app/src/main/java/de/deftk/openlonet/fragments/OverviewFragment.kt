package de.deftk.openlonet.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import de.deftk.lonet.api.request.UserApiRequest
import de.deftk.lonet.api.response.ResponseUtil
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.abstract.StartFragment
import de.deftk.openlonet.activities.StartActivity
import de.deftk.openlonet.adapter.OverviewAdapter
import de.deftk.openlonet.databinding.FragmentOverviewBinding
import de.deftk.openlonet.feature.AppFeature
import de.deftk.openlonet.feature.overview.AbstractOverviewElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverviewFragment: StartFragment() {

    companion object {
        private const val LOG_TAG = "OverviewFragment"
    }

    private lateinit var binding: FragmentOverviewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, bundle: Bundle?): View {
        binding = FragmentOverviewBinding.inflate(inflater, container, false)

        binding.overviewSwipeRefresh.setOnRefreshListener {
            binding.overviewList.adapter = null
            CoroutineScope(Dispatchers.IO).launch {
                refreshOverview()
            }
        }
        binding.overviewList.setOnItemClickListener { _, _, position, _ ->
            val item = binding.overviewList.getItemAtPosition(position) as AbstractOverviewElement
            val feature = AppFeature.getByOverviewClass(item::class.java)
            if (feature != null)
                (activity as StartActivity).displayFeatureFragment(feature)
        }
        CoroutineScope(Dispatchers.IO).launch {
            refreshOverview()
        }
        Log.i(LOG_TAG, "Created overview fragment")
        return binding.root
    }

    override fun getTitle(): String {
        return getString(R.string.overview)
    }

    private suspend fun refreshOverview() {
        try {
            val elements = mutableListOf<AbstractOverviewElement>()
            val request = UserApiRequest(AuthStore.getAppUser())
            val idMap = mutableMapOf<AppFeature, List<Int>>()
            AppFeature.values().forEach { feature ->
                if (feature.overviewBuilder != null) {
                    idMap[feature] = feature.overviewBuilder.appendRequests(request)
                }
            }
            val response = request.fireRequest().toJson()
            idMap.forEach { (feature, ids) ->
                elements.add(feature.overviewBuilder!!.createElementFromResponse(ids.map { Pair(it, ResponseUtil.getSubResponseResult(response, it)) }.toMap()))
            }

            withContext(Dispatchers.Main) {
                binding.overviewList.adapter = OverviewAdapter(requireContext(), elements)
                Log.i(LOG_TAG, "Initialized ${elements.size} overview elements")
                binding.progressOverview.visibility = ProgressBar.GONE
                binding.overviewSwipeRefresh.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    getString(R.string.overview_request_failed).format(e.message ?: e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}