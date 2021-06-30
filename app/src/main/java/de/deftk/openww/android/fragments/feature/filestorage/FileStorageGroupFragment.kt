package de.deftk.openww.android.fragments.feature.filestorage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.adapter.recycler.FileStorageAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentFileStorageBinding
import de.deftk.openww.android.viewmodel.FileStorageViewModel
import de.deftk.openww.android.viewmodel.UserViewModel

class FileStorageGroupFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val fileStorageViewModel: FileStorageViewModel by activityViewModels()

    private lateinit var binding: FragmentFileStorageBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFileStorageBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()

        val adapter = FileStorageAdapter()
        binding.fileList.adapter = adapter
        binding.fileList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        fileStorageViewModel.quotasResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value.toList())
                binding.fileEmpty.isVisible = response.value.isEmpty()
            } else if (response is Response.Failure) {
                //TODO handle error
                response.exception.printStackTrace()
            }
            binding.progressFileStorage.isVisible = false
            binding.fileStorageSwipeRefresh.isRefreshing = false
        }

        binding.fileStorageSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                fileStorageViewModel.loadQuotas(apiContext)
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                fileStorageViewModel.loadQuotas(apiContext)
            } else {
                binding.fileEmpty.isVisible = false
                adapter.submitList(emptyList())
                binding.progressFileStorage.isVisible = true
            }
        }

        return binding.root
    }

}