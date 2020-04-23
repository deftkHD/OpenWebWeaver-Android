package de.deftk.lonet.mobile.fragments

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.request.UserApiRequest
import de.deftk.lonet.api.response.ResponseUtil
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.activities.StartActivity
import de.deftk.lonet.mobile.adapter.OverviewAdapter
import de.deftk.lonet.mobile.feature.AppFeature
import de.deftk.lonet.mobile.feature.overview.AbstractOverviewElement
import kotlinx.android.synthetic.main.fragment_overview.*

class OverviewFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, bundle: Bundle?): View {
        OverviewLoader().execute(false)
        val view = inflater.inflate(R.layout.fragment_overview, container, false)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.overview_swipe_refresh)
        val list = view.findViewById<ListView>(R.id.overview_list)
        swipeRefresh.setOnRefreshListener {
            list.adapter = null
            OverviewLoader().execute(true)
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val item = list.getItemAtPosition(position) as AbstractOverviewElement
            val feature = AppFeature.getByOverviewClass(item::class.java)
            if (feature != null)
                (activity as StartActivity).displayFeatureFragment(feature)
        }
        return view
    }

    private inner class OverviewLoader: AsyncTask<Boolean, Void, List<AbstractOverviewElement>?>() {

        // a bit inefficient (although it gets cached later)
        override fun doInBackground(vararg params: Boolean?): List<AbstractOverviewElement>? {
            val elements = mutableListOf<AbstractOverviewElement>()
            val request = UserApiRequest(AuthStore.appUser)
            val idMap = mutableMapOf<AppFeature, List<Int>>()
            AppFeature.values().forEach { feature ->
                if (feature.overviewBuilder != null) {
                    idMap[feature] = feature.overviewBuilder.appendRequests(request)
                }
            }
            val response = try {
                request.fireRequest(params[0] == true).toJson()
            } catch (e:Exception) {
                e.printStackTrace()
                return null
            }
            idMap.forEach { (feature, ids) ->
                elements.add(feature.overviewBuilder!!.createElementFromResponse(ids.map { Pair(it, ResponseUtil.getSubResponseResult(response, it)) }.toMap()))
            }
            return elements
        }

        override fun onPostExecute(result: List<AbstractOverviewElement>?) {
            progress_overview?.visibility = ProgressBar.GONE
            overview_swipe_refresh?.isRefreshing = false
            if (context != null) {
                if (result != null) {
                    overview_list?.adapter = OverviewAdapter(context!!, result)
                } else {
                    Toast.makeText(context, "Failed to get overview information", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}