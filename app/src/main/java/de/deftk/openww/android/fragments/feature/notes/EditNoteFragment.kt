package de.deftk.openww.android.fragments.feature.notes

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentEditNoteBinding
import de.deftk.openww.android.feature.notes.NoteColors
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.NotesViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.feature.notes.INote
import de.deftk.openww.api.model.feature.notes.NoteColor

class EditNoteFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val notesViewModel: NotesViewModel by activityViewModels()
    private val args: EditNoteFragmentArgs by navArgs()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentEditNoteBinding
    private lateinit var note: INote

    private var editMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditNoteBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        binding.noteColor.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, NoteColors.values().map { getString(it.text) })

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (args.noteId != null) {
                    val foundNote = notesViewModel.notesResponse.value?.valueOrNull()?.firstOrNull { it.id == args.noteId }
                    if (foundNote == null) {
                        Reporter.reportException(R.string.error_note_not_found, args.noteId!!, requireContext())
                        navController.popBackStack()
                        return@observe
                    }
                    note = foundNote

                    binding.noteTitle.setText(note.getTitle())
                    binding.noteText.setText(note.getText())
                    binding.noteText.movementMethod = LinkMovementMethod.getInstance()
                    binding.noteText.transformationMethod = CustomTabTransformationMethod(binding.noteText.autoLinkMask)
                    binding.noteColor.setSelection(note.getColor()?.ordinal ?: 0)

                    editMode = true
                } else {
                    editMode = false
                }
            } else {
                navController.popBackStack(R.id.notesFragment, false)
            }
        }

        notesViewModel.editResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                notesViewModel.resetEditResponse()

            if (response is Response.Success) {
                ViewCompat.getWindowInsetsController(requireView())?.hide(WindowInsetsCompat.Type.ime())
                navController.popBackStack()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_save_changes_failed, response.exception, requireContext())
            }
        }

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_save_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_item_save) {
            val apiContext = userViewModel.apiContext.value ?: return false
            val color = NoteColor.values()[binding.noteColor.selectedItemPosition]
            if (editMode) {
                notesViewModel.editNote(note, binding.noteTitle.text.toString(), binding.noteText.text.toString(), color, apiContext)
            } else {
                notesViewModel.addNote(binding.noteTitle.text.toString(), binding.noteText.text.toString(), color, apiContext)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}