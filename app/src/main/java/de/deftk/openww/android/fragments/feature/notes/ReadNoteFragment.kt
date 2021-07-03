package de.deftk.openww.android.fragments.feature.notes

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentReadNoteBinding
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.NotesViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.notes.INote
import java.text.DateFormat

class ReadNoteFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val notesViewModel: NotesViewModel by activityViewModels()
    private val args: ReadNoteFragmentArgs by navArgs()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentReadNoteBinding
    private lateinit var note: INote

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadNoteBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()

        notesViewModel.notesResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                val searched = response.value.firstOrNull { it.id == args.noteId }
                if (searched == null) {
                    Reporter.reportException(R.string.error_note_not_found, args.noteId, requireContext())
                    navController.popBackStack()
                    return@observe
                }
                note = searched

                binding.noteTitle.text = note.getTitle()
                binding.noteDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(note.created.date)
                binding.noteText.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(note.getText()))
                binding.noteText.movementMethod = LinkMovementMethod.getInstance()
                binding.noteText.transformationMethod = CustomTabTransformationMethod(binding.noteText.autoLinkMask)
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_notes_failed, response.exception, requireContext())
            }
        }

        notesViewModel.deleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                notesViewModel.resetDeleteResponse()
            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }

        binding.fabEditNote.setOnClickListener {
            navController.navigate(ReadNoteFragmentDirections.actionReadNoteFragmentToEditNoteFragment(note.id, getString(R.string.edit_note)))
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                binding.fabEditNote.isVisible = apiContext.getUser().effectiveRights.contains(Permission.NOTES_WRITE) || apiContext.getUser().effectiveRights.contains(Permission.NOTES_ADMIN)
            } else {
                navController.popBackStack(R.id.notesFragment, false)
            }
        }

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val user = userViewModel.apiContext.value?.getUser() ?: return
        if (user.effectiveRights.contains(Permission.NOTES_WRITE) || user.effectiveRights.contains(Permission.NOTES_ADMIN))
            inflater.inflate(R.menu.simple_edit_item_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                val action = ReadNoteFragmentDirections.actionReadNoteFragmentToEditNoteFragment(note.id, getString(R.string.edit_note))
                navController.navigate(action)
                true
            }
            R.id.menu_item_delete -> {
                val apiContext = userViewModel.apiContext.value ?: return false
                notesViewModel.deleteNote(note, apiContext)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}