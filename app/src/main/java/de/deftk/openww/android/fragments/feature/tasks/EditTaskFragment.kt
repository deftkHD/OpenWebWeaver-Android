package de.deftk.openww.android.fragments.feature.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.ScopeSelectionAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentEditTaskBinding
import de.deftk.openww.android.fragments.ContextualFragment
import de.deftk.openww.android.utils.AndroidUtil
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.TasksViewModel
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.tasks.ITask
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class EditTaskFragment : ContextualFragment(true) {

    //TODO ability to remove start/due date

    private val args: EditTaskFragmentArgs by navArgs()
    private val tasksViewModel: TasksViewModel by activityViewModels()

    private lateinit var binding: FragmentEditTaskBinding

    private var task: ITask? = null
    private var scope: IOperatingScope? = null
    private var editMode: Boolean = false
    private var startDate: Date? = null
    private var dueDate: Date? = null
    private var effectiveGroups: List<IGroup>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditTaskBinding.inflate(inflater, container, false)

        tasksViewModel.allTasksResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                setUIState(UIState.READY)
                if (args.groupId != null && args.taskId != null) {
                    // edit existing
                    editMode = true
                    setTitle(R.string.edit_task)

                    val foundTask = tasksViewModel.allTasksResponse.value?.valueOrNull()?.firstOrNull { it.first.id == args.taskId && it.second.login == args.groupId }
                    if (foundTask == null) {
                        Reporter.reportException(R.string.error_task_not_found, args.taskId!!, requireContext())
                        navController.popBackStack()
                        return@observe
                    }
                    task = foundTask.first
                    scope = foundTask.second
                    val task = foundTask.first

                    binding.taskTitle.setText(task.title)
                    if (effectiveGroups != null)
                        binding.taskGroup.setSelection(effectiveGroups!!.indexOf(scope))
                    binding.taskCompleted.isChecked = task.completed
                    binding.taskText.setText(task.description)

                    startDate = task.startDate
                    if (startDate != null)
                        binding.taskStart.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(startDate!!))

                    dueDate = task.dueDate
                    if (dueDate != null)
                        binding.taskDue.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(dueDate!!))
                } else {
                    // add new
                    editMode = false
                    setTitle(R.string.new_task)
                    binding.taskGroup.isEnabled = true
                }
                invalidateOptionsMenu()
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_get_tasks_failed, response.exception, requireContext())
                navController.popBackStack()
                return@observe
            }
        }

        loginViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (apiContext.user.getGroups().none { it.effectiveRights.contains(Permission.TASKS_WRITE) } && apiContext.user.getGroups().none { it.effectiveRights.contains(Permission.TASKS_ADMIN) }) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }

                effectiveGroups = apiContext.user.getGroups().filter { it.effectiveRights.contains(Permission.TASKS_WRITE) || it.effectiveRights.contains(Permission.TASKS_ADMIN) }
                binding.taskGroup.adapter = ScopeSelectionAdapter(requireContext(), effectiveGroups!!)

                tasksViewModel.loadTasks(true, apiContext)
                setUIState(UIState.LOADING)
            } else {
                binding.taskTitle.setText("")
                binding.taskGroup.adapter = null
                binding.taskCompleted.isChecked = false
                binding.taskText.setText("")
                binding.taskStart.setText("")
                startDate = null
                binding.taskDue.setText("")
                dueDate = null
                setUIState(UIState.DISABLED)
            }
        }

        tasksViewModel.postResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                tasksViewModel.resetPostResponse() // mark as handled

            if (response is Response.Success) {
                setUIState(UIState.READY)
                AndroidUtil.hideKeyboard(requireActivity(), requireView())
                navController.popBackStack()
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_save_changes_failed, response.exception, requireContext())
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
                        Toast.makeText(requireContext(), R.string.task_start_before_due, Toast.LENGTH_SHORT).show()
                    } else {
                        dueDate = calendar.time
                        binding.taskDue.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(dueDate!!))
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.edit_options_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.edit_options_item_save) {
            val apiContext = loginViewModel.apiContext.value ?: return true
            val title = binding.taskTitle.text.toString()
            val selectedGroup = binding.taskGroup.selectedItem as? IOperatingScope?
            val completed = binding.taskCompleted.isChecked
            val description = binding.taskText.text.toString()

            if (editMode) {
                if (task != null && scope != null) {
                    tasksViewModel.editTask(task!!, title, description, completed, startDate, dueDate, scope!!, apiContext)
                    setUIState(UIState.LOADING)
                }
            } else {
                scope = apiContext.user.getGroups().firstOrNull { it.login == selectedGroup?.login } ?: return true
                if (scope != null) {
                    tasksViewModel.addTask(title, description, completed, startDate, dueDate, scope!!, apiContext)
                    setUIState(UIState.LOADING)
                }
            }
            return true
        }
        return false
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.taskCompleted.isEnabled = newState == UIState.READY
        binding.taskDue.isEnabled = newState == UIState.READY
        binding.taskGroup.isEnabled = newState == UIState.READY
        binding.taskStart.isEnabled = newState == UIState.READY
        binding.taskText.isEnabled = newState == UIState.READY
        binding.taskTitle.isEnabled = newState == UIState.READY
    }
}




