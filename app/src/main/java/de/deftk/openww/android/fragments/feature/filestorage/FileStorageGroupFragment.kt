package de.deftk.openww.android.fragments.feature.filestorage

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.FileStorageAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentFileStorageBinding
import de.deftk.openww.android.feature.LaunchMode
import de.deftk.openww.android.filter.FileStorageQuotaFilter
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.ISearchProvider
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.FileStorageViewModel
import de.deftk.openww.android.viewmodel.UserViewModel

class FileStorageGroupFragment : AbstractFragment(true), ISearchProvider {

    private val userViewModel: UserViewModel by activityViewModels()
    private val fileStorageViewModel: FileStorageViewModel by activityViewModels()

    private lateinit var binding: FragmentFileStorageBinding
    private lateinit var searchView: SearchView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFileStorageBinding.inflate(inflater, container, false)

        val pasteMode = LaunchMode.getLaunchMode(requireActivity().intent) == LaunchMode.FILE_UPLOAD
        val adapter = FileStorageAdapter(pasteMode)
        binding.fileList.adapter = adapter
        binding.fileList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        fileStorageViewModel.filteredQuotasResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value.toList())
                setUIState(if (response.value.isEmpty()) UIState.EMPTY else UIState.READY)
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_get_quotas_failed, response.exception, requireContext())
            }
        }

        binding.fileStorageSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                fileStorageViewModel.loadQuotas(apiContext)
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (fileStorageViewModel.allQuotasResponse.value == null) {
                    fileStorageViewModel.loadQuotas(apiContext)
                    setUIState(UIState.LOADING)
                }
            } else {
                adapter.submitList(emptyList())
                setUIState(UIState.DISABLED)
            }
        }

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_options_menu, menu)
        val searchItem = menu.findItem(R.id.list_options_item_search)
        searchView = searchItem.actionView as SearchView
        searchView.setQuery(fileStorageViewModel.quotaFilter.value?.smartSearchCriteria?.value, false) // restore recent search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filter = FileStorageQuotaFilter()
                filter.smartSearchCriteria.value = newText
                fileStorageViewModel.quotaFilter.value = filter
                return true
            }
        })
    }

    override fun onSearchBackPressed(): Boolean {
        return if (searchView.isIconified) {
            false
        } else {
            searchView.isIconified = true
            searchView.setQuery(null, true)
            true
        }
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.fileStorageSwipeRefresh.isEnabled = newState.swipeRefreshEnabled
        binding.fileStorageSwipeRefresh.isRefreshing = newState.refreshing
        binding.fileList.isEnabled = newState.listEnabled
        binding.fileEmpty.isVisible = newState.showEmptyIndicator
    }
}