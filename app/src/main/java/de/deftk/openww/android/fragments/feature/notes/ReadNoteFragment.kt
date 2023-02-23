package de.deftk.openww.android.fragments.feature.notes

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentReadNoteBinding
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.NotesViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.notes.INote
import java.text.DateFormat

class ReadNoteFragment : AbstractFragment(true) {

    private val userViewModel: UserViewModel by activityViewModels()
    private val notesViewModel: NotesViewModel by activityViewModels()
    private val args: ReadNoteFragmentArgs by navArgs()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentReadNoteBinding
    private lateinit var note: INote

    private var deleted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadNoteBinding.inflate(inflater, container, false)

        notesViewModel.allNotesResponse.observe(viewLifecycleOwner) { response ->
            if (deleted)
                return@observe

            if (response is Response.Success) {
                setUIState(UIState.READY)
                val searched = response.value.firstOrNull { it.id == args.noteId }
                if (searched == null) {
                    Reporter.reportException(R.string.error_note_not_found, args.noteId, requireContext())
                    navController.popBackStack()
                    return@observe
                }
                note = searched

                binding.noteTitle.text = note.title
                binding.noteDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(note.created.date)
                binding.noteText.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(note.text), userViewModel.apiContext.value?.user?.login, navController)
                binding.noteText.movementMethod = LinkMovementMethod.getInstance()
                binding.noteText.transformationMethod = CustomTabTransformationMethod(binding.noteText.autoLinkMask)
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_get_notes_failed, response.exception, requireContext())
                navController.popBackStack()
                return@observe
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (!Feature.NOTES.isAvailable(apiContext.user.effectiveRights)) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack(R.id.notesFragment, false)
                    return@observe
                }
                binding.fabEditNote.isVisible = apiContext.user.effectiveRights.contains(Permission.NOTES_WRITE) || apiContext.user.effectiveRights.contains(Permission.NOTES_ADMIN)
                if (notesViewModel.allNotesResponse.value == null) {
                    notesViewModel.loadNotes(apiContext)
                    setUIState(UIState.LOADING)
                }
            } else {
                binding.noteTitle.text = ""
                binding.noteText.text = ""
                binding.noteDate.text = ""
                binding.fabEditNote.isVisible = false
                setUIState(UIState.DISABLED)
            }
        }

        notesViewModel.deleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                notesViewModel.resetDeleteResponse()

            if (response is Response.Success) {
                setUIState(UIState.READY)
                deleted = true
                navController.popBackStack()
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }

        binding.fabEditNote.setOnClickListener {
            navController.navigate(ReadNoteFragmentDirections.actionReadNoteFragmentToEditNoteFragment(note.id, getString(R.string.edit_note)))
        }

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        val user = userViewModel.apiContext.value?.user ?: return
        if (user.effectiveRights.contains(Permission.NOTES_WRITE) || user.effectiveRights.contains(Permission.NOTES_ADMIN))
            menuInflater.inflate(R.menu.notes_context_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.notes_context_item_edit -> {
                val action = ReadNoteFragmentDirections.actionReadNoteFragmentToEditNoteFragment(note.id, getString(R.string.edit_note))
                navController.navigate(action)
                true
            }
            R.id.notes_context_item_delete -> {
                val apiContext = userViewModel.apiContext.value ?: return false
                notesViewModel.deleteNote(note, apiContext)
                setUIState(UIState.LOADING)
                true
            }
            else -> false
        }
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.fabEditNote.isEnabled = newState == UIState.READY
    }
}