package de.deftk.openww.android.fragments.feature.notes

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentEditNoteBinding
import de.deftk.openww.android.feature.notes.NoteColors
import de.deftk.openww.android.fragments.ContextualFragment
import de.deftk.openww.android.utils.AndroidUtil
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.NotesViewModel
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.notes.INote
import de.deftk.openww.api.model.feature.notes.NoteColor

class EditNoteFragment : ContextualFragment(true) {

    private val notesViewModel: NotesViewModel by activityViewModels()
    private val args: EditNoteFragmentArgs by navArgs()

    private lateinit var binding: FragmentEditNoteBinding
    private lateinit var note: INote

    private var editMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditNoteBinding.inflate(inflater, container, false)

        notesViewModel.allNotesResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                setUIState(UIState.READY)
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
                    setTitle(R.string.edit_note)
                } else {
                    editMode = false
                    setTitle(R.string.add_note)
                }
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_get_notifications_failed, response.exception, requireContext())
                navController.popBackStack()
                return@observe
            }
        }

        loginViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (!apiContext.user.effectiveRights.contains(Permission.NOTES_WRITE) && !apiContext.user.effectiveRights.contains(Permission.NOTES_ADMIN)) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }
                binding.noteColor.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, NoteColors.values().map { getString(it.text) })

                if (notesViewModel.allNotesResponse.value == null) {
                    notesViewModel.loadNotes(apiContext)
                    setUIState(UIState.LOADING)
                }
            } else {
                binding.noteTitle.setText("")
                binding.noteText.setText("")
                binding.noteColor.adapter = null
                setUIState(UIState.DISABLED)
            }
        }

        notesViewModel.editResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                notesViewModel.resetEditResponse()

            if (response is Response.Success) {
                setUIState(UIState.READY)
                AndroidUtil.hideKeyboard(requireActivity(), requireView())
                navController.popBackStack()
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
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
            val apiContext = loginViewModel.apiContext.value ?: return false
            val color = NoteColor.values()[binding.noteColor.selectedItemPosition]
            if (editMode) {
                notesViewModel.editNote(note, binding.noteTitle.text.toString(), binding.noteText.text.toString(), color, apiContext)
                setUIState(UIState.LOADING)
            } else {
                notesViewModel.addNote(binding.noteTitle.text.toString(), binding.noteText.text.toString(), color, apiContext)
                setUIState(UIState.LOADING)
            }
            return true
        }
        return false
    }


    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.noteColor.isEnabled = newState == UIState.READY
        binding.noteText.isEnabled = newState == UIState.READY
        binding.noteTitle.isEnabled = newState == UIState.READY
    }
}