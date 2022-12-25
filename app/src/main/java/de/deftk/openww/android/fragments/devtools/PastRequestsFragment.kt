package de.deftk.openww.android.fragments.devtools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.adapter.recycler.ActionModeAdapter
import de.deftk.openww.android.adapter.recycler.RequestAdapter
import de.deftk.openww.android.databinding.FragmentPastRequestsBinding
import de.deftk.openww.android.feature.devtools.PastRequest
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.fragments.ActionModeClickListener
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.viewmodel.UserViewModel

class PastRequestsFragment : AbstractFragment(true), ActionModeClickListener<RequestAdapter.RequestViewHolder> {

    private val userViewModel by activityViewModels<UserViewModel>()
    private val navController by lazy { findNavController() }
    private val adapter by lazy { RequestAdapter(this) }

    private lateinit var binding: FragmentPastRequestsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPastRequestsBinding.inflate(inflater, container, false)

        binding.pastRequestsList.adapter = adapter
        binding.pastRequestsList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        userViewModel.pastRequests.observe(viewLifecycleOwner) { pastRequests ->
            adapter.submitList(pastRequests)
        }

        registerForContextMenu(binding.pastRequestsList)
        return binding.root
    }

    override fun onUIStateChanged(enabled: Boolean) {
        binding.pastRequestsList.isEnabled = enabled
    }

    override fun onClick(view: View, viewHolder: RequestAdapter.RequestViewHolder) {
        val id = viewHolder.binding.requestId ?: return
        navController.navigate(PastRequestsFragmentDirections.actionPastRequestsFragmentToPastRequestFragment(id))
    }

    override fun onLongClick(view: View, viewHolder: RequestAdapter.RequestViewHolder) {}
}