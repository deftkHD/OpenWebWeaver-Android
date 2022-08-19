package de.deftk.openww.android.fragments.feature.notes

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.ArrayAdapter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentEditNoteBinding
import de.deftk.openww.android.feature.notes.NoteColors
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.NotesViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.notes.INote
import de.deftk.openww.api.model.feature.notes.NoteColor

class EditNoteFragment : AbstractFragment(true) {

    private val userViewModel: UserViewModel by activityViewModels()
    private val notesViewModel: NotesViewModel by activityViewModels()
    private val args: EditNoteFragmentArgs by navArgs()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentEditNoteBinding
    private lateinit var note: INote

    private var editMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditNoteBinding.inflate(inflater, container, false)

        notesViewModel.allNotesResponse.observe(viewLifecycleOwner) { response ->
            enableUI(true)
            if (response is Response.Success) {
                if (args.noteId != null) {
                    val foundNote = response.value.firstOrNull { it.id == args.noteId }
                    if (foundNote == null) {
                        Reporter.reportException(R.string.error_note_not_found, args.noteId!!, requireContext())
                        navController.popBackStack()
                        return@observe
                    }
                    note = foundNote

                    binding.noteTitle.setText(note.title)
                    binding.noteText.setText(note.text)
                    binding.noteText.movementMethod = LinkMovementMethod.getInstance()
                    binding.noteText.transformationMethod = CustomTabTransformationMethod(binding.noteText.autoLinkMask)
                    binding.noteColor.setSelection(note.color?.ordinal ?: 0)

                    editMode = true
                } else {
                    editMode = false
                }
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_notifications_failed, response.exception, requireContext())
                navController.popBackStack()
                return@observe
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (!apiContext.user.effectiveRights.contains(Permission.NOTES_WRITE) && !apiContext.user.effectiveRights.contains(Permission.NOTES_ADMIN)) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }
                binding.noteColor.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, NoteColors.values().map { getString(it.text) })

                notesViewModel.loadNotes(apiContext)
                if (notesViewModel.allNotesResponse.value == null)
                    enableUI(false)
            } else {
                binding.noteTitle.setText("")
                binding.noteText.setText("")
                binding.noteColor.adapter = null
                enableUI(false)
            }
        }

        notesViewModel.editResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                notesViewModel.resetEditResponse()
            enableUI(true)

            if (response is Response.Success) {
                ViewCompat.getWindowInsetsController(requireView())?.hide(WindowInsetsCompat.Type.ime())
                navController.popBackStack()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_save_changes_failed, response.exception, requireContext())
            }
        }

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.edit_options_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.edit_options_item_save) {
            val apiContext = userViewModel.apiContext.value ?: return false
            val color = NoteColor.values()[binding.noteColor.selectedItemPosition]
            if (editMode) {
                notesViewModel.editNote(note, binding.noteTitle.text.toString(), binding.noteText.text.toString(), color, apiContext)
                enableUI(false)
            } else {
                notesViewModel.addNote(binding.noteTitle.text.toString(), binding.noteText.text.toString(), color, apiContext)
                enableUI(false)
            }
            return true
        }
        return false
    }

    override fun onUIStateChanged(enabled: Boolean) {
        binding.noteColor.isEnabled = enabled
        binding.noteText.isEnabled = enabled
        binding.noteTitle.isEnabled = enabled
    }
}