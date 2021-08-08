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
            enableUI(true)
            if (deleted)
                return@observe

            if (response is Response.Success) {
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
                notesViewModel.loadNotes(apiContext)
                if (notesViewModel.allNotesResponse.value == null)
                    enableUI(false)
            } else {
                binding.noteTitle.text = ""
                binding.noteText.text = ""
                binding.noteDate.text = ""
                binding.fabEditNote.isVisible = false
                enableUI(false)
            }
        }

        notesViewModel.deleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                notesViewModel.resetDeleteResponse()
            enableUI(true)

            if (response is Response.Success) {
                deleted = true
                navController.popBackStack()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }

        binding.fabEditNote.setOnClickListener {
            navController.navigate(ReadNoteFragmentDirections.actionReadNoteFragmentToEditNoteFragment(note.id, getString(R.string.edit_note)))
        }

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val user = userViewModel.apiContext.value?.user ?: return
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
                enableUI(false)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onUIStateChanged(enabled: Boolean) {
        binding.fabEditNote.isEnabled = enabled
    }
}