package de.deftk.openww.android.fragments.devtools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.adapter.recycler.ExceptionAdapter
import de.deftk.openww.android.databinding.FragmentExceptionsBinding
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.fragments.ActionModeClickListener
import de.deftk.openww.android.utils.DebugUtil

class ExceptionsFragment : AbstractFragment(true), ActionModeClickListener<ExceptionAdapter.ReportViewHolder> {

    private val navController by lazy { findNavController() }
    private val adapter by lazy { ExceptionAdapter(this) }

    private lateinit var binding: FragmentExceptionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentExceptionsBinding.inflate(inflater, container, false)
        binding.exceptionList.adapter = adapter
        binding.exceptionList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        DebugUtil.exceptions.observe(viewLifecycleOwner) { exceptions ->
            adapter.submitList(exceptions)
            if (exceptions.isNotEmpty()) {
                setUIState(UIState.READY)
            } else {
                setUIState(UIState.EMPTY)
            }
        }

        registerForContextMenu(binding.exceptionList)
        return binding.root
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.exceptionList.isEnabled = newState.listEnabled
        binding.exceptionsEmpty.isVisible = newState.showEmptyIndicator
    }

    override fun onClick(view: View, viewHolder: ExceptionAdapter.ReportViewHolder) {
        val id = viewHolder.binding.exceptionId ?: return
        navController.navigate(ExceptionsFragmentDirections.actionExceptionsFragmentToExceptionFragment(id))
    }

    override fun onLongClick(view: View, viewHolder: ExceptionAdapter.ReportViewHolder) {}
}