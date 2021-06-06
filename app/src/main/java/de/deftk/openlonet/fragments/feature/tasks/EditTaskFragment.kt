package de.deftk.openlonet.fragments.feature.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.lonet.api.model.IOperatingScope
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.tasks.ITask
import de.deftk.openlonet.R
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.databinding.FragmentEditTaskBinding
import de.deftk.openlonet.viewmodel.TasksViewModel
import de.deftk.openlonet.viewmodel.UserViewModel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class EditTaskFragment : Fragment() {

    //TODO ability to remove start/due date

    private val args: EditTaskFragmentArgs by navArgs()
    private val tasksViewModel: TasksViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentEditTaskBinding
    private lateinit var task: ITask
    private lateinit var operator: IOperatingScope

    private var editMode: Boolean = false
    private var startDate: Date? = null
    private var dueDate: Date? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditTaskBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val effectiveGroups = userViewModel.apiContext.value?.getUser()?.getGroups()?.filter { it.effectiveRights.contains(Permission.BOARD_ADMIN) } ?: emptyList()
        binding.taskGroup.adapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, effectiveGroups.map { it.login })

        if (args.groupId != null && args.taskId != null) {
            // edit existing
            editMode = true
            tasksViewModel.tasksResponse.observe(viewLifecycleOwner) { resource ->
                if (resource is Response.Success) {
                    resource.value.firstOrNull { it.first.id == args.taskId && it.second.login == args.groupId }?.apply {
                        task = first
                        operator = second

                        binding.taskTitle.setText(task.getTitle())
                        binding.taskGroup.setSelection(effectiveGroups.indexOf(operator))
                        binding.taskCompleted.isChecked = task.isCompleted()
                        binding.taskText.setText(task.getDescription())

                        startDate = task.getStartDate()
                        if (startDate != null)
                            binding.taskStart.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(startDate!!))

                        dueDate = task.getEndDate()
                        if (dueDate != null)
                            binding.taskDue.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(dueDate!!))
                    }
                } else if (resource is Response.Failure) {
                    resource.exception.printStackTrace()
                    //TODO handle error
                }
            }
        } else {
            // add new
            editMode = false
            binding.taskGroup.isEnabled = true
        }

        tasksViewModel.postResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                tasksViewModel.resetPostResponse() // mark as handled

            if (response is Response.Success) {
                ViewCompat.getWindowInsetsController(requireView())?.hide(WindowInsetsCompat.Type.ime())
                navController.popBackStack()
            } else if (response is Response.Failure) {
                //TODO handle error
                response.exception.printStackTrace()
            }
        }

        // setup date pickers

        binding.taskStart.inputType = InputType.TYPE_NULL
        binding.taskStart.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (binding.taskStart.text.toString().isNotBlank())
                calendar.time = SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).parse(binding.taskStart.text.toString()) ?: Date()
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                TimePickerDialog(requireContext(), { _, hour, minute ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)

                    if (dueDate != null && calendar.timeInMillis > dueDate!!.time) {
                        //TODO snackbar
                        Toast.makeText(requireContext(), R.string.task_start_before_due, Toast.LENGTH_SHORT).show()
                    } else {
                        startDate = calendar.time
                        binding.taskStart.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(startDate!!))
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.taskDue.inputType = InputType.TYPE_NULL
        binding.taskDue.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (binding.taskDue.text.toString().isNotBlank())
                calendar.time = SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).parse(binding.taskDue.text.toString()) ?: Date()
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                TimePickerDialog(requireContext(), { _, hour, minute ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)

                    if (startDate != null && calendar.timeInMillis < startDate!!.time) {
                        //TODO snackbar
                        Toast.makeText(requireContext(), R.string.task_start_before_due, Toast.LENGTH_SHORT).show()
                    } else {
                        dueDate = calendar.time
                        binding.taskDue.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(dueDate!!))
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_save_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.save) {
            val apiContext = userViewModel.apiContext.value ?: return false
            val title = binding.taskTitle.text.toString()
            val selectedGroup = binding.taskGroup.selectedItem
            val completed = binding.taskCompleted.isChecked
            val description = binding.taskText.text.toString()

            if (editMode) {
                tasksViewModel.editTask(task, title, description, completed, startDate, dueDate, operator, apiContext)
            } else {
                operator = apiContext.getUser().getGroups().firstOrNull { it.login == selectedGroup } ?: return false
                tasksViewModel.addTask(title, description, completed, startDate, dueDate, operator, apiContext)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}



