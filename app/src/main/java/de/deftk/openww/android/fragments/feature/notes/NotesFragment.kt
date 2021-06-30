package de.deftk.openww.android.fragments.feature.notes

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.NoteAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentNotesBinding
import de.deftk.openww.android.viewmodel.NotesViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.Permission

class NotesFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val notesViewModel: NotesViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentNotesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNotesBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()

        val adapter = NoteAdapter()
        binding.notesList.adapter = adapter
        binding.notesList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        notesViewModel.notesResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value)
                binding.notesEmpty.isVisible = response.value.isEmpty()
            } else if (response is Response.Failure) {
                //TODO handle error
                response.exception.printStackTrace()
            }
            binding.progressNotes.isVisible = false
            binding.notesSwipeRefresh.isRefreshing = false
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
                binding.fabAddNote.isVisible = apiContext.getUser().effectiveRights.contains(Permission.NOTES_WRITE) || apiContext.getUser().effectiveRights.contains(Permission.NOTES_ADMIN)

                notesViewModel.loadNotes(apiContext)
            } else {
                navController.popBackStack()
            }
        }

        registerForContextMenu(binding.notesList)
        return binding.root
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            val user = userViewModel.apiContext.value?.getUser() ?: return
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

}