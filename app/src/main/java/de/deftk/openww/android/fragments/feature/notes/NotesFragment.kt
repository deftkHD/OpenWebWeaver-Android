package de.deftk.openww.android.fragments.feature.notes

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.activities.MainActivity
import de.deftk.openww.android.adapter.recycler.ActionModeAdapter
import de.deftk.openww.android.adapter.recycler.NoteAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentNotesBinding
import de.deftk.openww.android.filter.NoteFilter
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.utils.ISearchProvider
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.NotesViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.notes.INote

class NotesFragment : ActionModeFragment<INote, NoteAdapter.NoteViewHolder>(R.menu.notes_actionmode_menu), ISearchProvider {

    private val userViewModel: UserViewModel by activityViewModels()
    private val notesViewModel: NotesViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentNotesBinding
    private lateinit var searchView: SearchView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNotesBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        (requireActivity() as? MainActivity?)?.searchProvider = this

        binding.notesList.adapter = adapter
        binding.notesList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        notesViewModel.filteredNotesResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value)
                binding.notesEmpty.isVisible = response.value.isEmpty()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_notes_failed, response.exception, requireContext())
            }
            binding.progressNotes.isVisible = false
            binding.notesSwipeRefresh.isRefreshing = false
        }

        notesViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                notesViewModel.resetBatchDeleteResponse()

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
                binding.progressNotes.isVisible = false
            } else {
                actionMode?.finish()
            }
        }

        notesViewModel.deleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                notesViewModel.resetDeleteResponse() // mark as handled

            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }

        binding.notesSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                notesViewModel.loadNotes(apiContext)
            }
        }

        binding.fabAddNote.setOnClickListener {
            navController.navigate(NotesFragmentDirections.actionNotesFragmentToEditNoteFragment(null, getString(R.string.add_note)))
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                binding.fabAddNote.isVisible = apiContext.user.effectiveRights.contains(Permission.NOTES_WRITE) || apiContext.user.effectiveRights.contains(Permission.NOTES_ADMIN)

                notesViewModel.loadNotes(apiContext)
            } else {
                navController.popBackStack()
            }
        }

        setHasOptionsMenu(true)
        registerForContextMenu(binding.notesList)
        return binding.root
    }

    override fun createAdapter(): ActionModeAdapter<INote, NoteAdapter.NoteViewHolder> {
        return NoteAdapter(this)
    }

    override fun onItemClick(view: View, viewHolder: NoteAdapter.NoteViewHolder) {
        navController.navigate(NotesFragmentDirections.actionNotesFragmentToReadNoteFragment(viewHolder.binding.note!!.id))
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val user = userViewModel.apiContext.value?.user
        val canModify = user?.effectiveRights?.contains(Permission.NOTES_WRITE) == true || user?.effectiveRights?.contains(Permission.NOTES_ADMIN) == true
        menu.findItem(R.id.notes_action_delete).isEnabled = canModify
        return super.onPrepareActionMode(mode, menu)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.notes_action_delete -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    notesViewModel.batchDelete(adapter.selectedItems.map { it.binding.note!! }, apiContext)
                    binding.progressNotes.isVisible = true
                }
            }
            else -> return false
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        searchView = searchItem.actionView as SearchView
        searchView.setQuery(notesViewModel.filter.value?.smartSearchCriteria?.value, false) // restore recent search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filter = NoteFilter()
                filter.smartSearchCriteria.value = newText
                notesViewModel.filter.value = filter
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
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

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            val user = userViewModel.apiContext.value?.user ?: return
            if (user.effectiveRights.contains(Permission.NOTES_WRITE) || user.effectiveRights.contains(Permission.NOTES_ADMIN)) {
                requireActivity().menuInflater.inflate(R.menu.simple_edit_item_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.notesList.adapter as NoteAdapter
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                val note = adapter.getItem(menuInfo.position)
                val action = NotesFragmentDirections.actionNotesFragmentToEditNoteFragment(note.id, getString(R.string.edit_note))
                navController.navigate(action)
                true
            }
            R.id.menu_item_delete -> {
                val note = adapter.getItem(menuInfo.position)
                val apiContext = userViewModel.apiContext.value ?: return false
                notesViewModel.deleteNote(note, apiContext)
                true
            }
            else -> false
        }
    }

    override fun onDestroy() {
        (requireActivity() as? MainActivity?)?.searchProvider = null
        super.onDestroy()
    }

}