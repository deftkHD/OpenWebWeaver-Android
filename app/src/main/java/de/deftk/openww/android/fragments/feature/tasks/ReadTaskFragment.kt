package de.deftk.openww.android.fragments.feature.tasks

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentReadTaskBinding
import de.deftk.openww.android.fragments.ContextualFragment
import de.deftk.openww.android.utils.CalendarUtil
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.TasksViewModel
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.tasks.ITask
import java.text.DateFormat

class ReadTaskFragment : ContextualFragment(true) {

    private val args: ReadTaskFragmentArgs by navArgs()
    private val tasksViewModel: TasksViewModel by activityViewModels()

    private lateinit var binding: FragmentReadTaskBinding

    private var task: ITask? = null
    private var scope: IOperatingScope? = null
    private var deleted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadTaskBinding.inflate(inflater, container, false)

        tasksViewModel.allTasksResponse.observe(viewLifecycleOwner) { response ->
            if (deleted)
                return@observe

            if (response is Response.Success) {
                setUIState(UIState.READY)
                val foundTask = response.value.firstOrNull { it.first.id == args.taskId && it.second.login == args.groupId }
                if (foundTask == null) {
                    Reporter.reportException(R.string.error_task_not_found, args.taskId, requireContext())
                    navController.popBackStack()
                    return@observe
                }

                task = foundTask.first
                scope = foundTask.second
                val task = foundTask.first
                val scope = foundTask.second

                binding.taskTitle.text = task.title
                binding.taskAuthor.text = task.created.member.name
                binding.taskGroup.text = scope.name
                if (task.created.date != null) {
                    binding.taskCreated.text = String.format(getString(R.string.created_date), DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(task.created.date!!))
                } else {
                    binding.taskCreated.isVisible = false
                }
                binding.taskDue.text = String.format(getString(R.string.until_date), if (task.dueDate != null) DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(task.dueDate!!) else getString(R.string.not_set))
                binding.taskDetail.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(task.description), scope.login, navController)
                binding.taskDetail.movementMethod = LinkMovementMethod.getInstance()
                binding.taskDetail.transformationMethod = CustomTabTransformationMethod(binding.taskDetail.autoLinkMask)

                binding.fabEditTask.isVisible = scope.effectiveRights.contains(Permission.TASKS_WRITE) || scope.effectiveRights.contains(Permission.TASKS_ADMIN)
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_get_tasks_failed, response.exception, requireContext())
                navController.popBackStack()
            }
            invalidateOptionsMenu()
        }
        tasksViewModel.postResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                tasksViewModel.resetPostResponse() // mark as handled

            if (response is Response.Success) {
                setUIState(UIState.READY)
                deleted = true
                navController.popBackStack()
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }
        binding.fabEditTask.setOnClickListener {
            if (task != null && scope != null) {
                val action = ReadTaskFragmentDirections.actionReadTaskFragmentToEditTaskFragment(task!!.id, scope!!.login)
                navController.navigate(action)
            }
        }
        loginViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (apiContext.user.getGroups().none { Feature.TASKS.isAvailable(it.effectiveRights) }) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }
                if (tasksViewModel.allTasksResponse.value == null) {
                    tasksViewModel.loadTasks(true, apiContext)
                    setUIState(UIState.LOADING)
                }
            } else {
                binding.taskTitle.text = ""
                binding.taskAuthor.text = ""
                binding.taskCreated.text = ""
                binding.taskDetail.text = ""
                binding.taskDue.text = ""
                binding.taskGroup.text = ""
                binding.fabEditTask.isVisible = false
                setUIState(UIState.DISABLED)
            }
        }

        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (scope != null) {
            menuInflater.inflate(R.menu.tasks_context_menu, menu)
            val canEdit = scope!!.effectiveRights.contains(Permission.TASKS_WRITE) || scope!!.effectiveRights.contains(Permission.TASKS_ADMIN)
            menu.findItem(R.id.tasks_context_item_edit).isVisible = canEdit
            menu.findItem(R.id.tasks_context_item_delete).isVisible = canEdit
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.tasks_context_item_ignore -> {
                if (task != null && scope != null) {
                    loginViewModel.apiContext.value?.also { apiContext ->
                        tasksViewModel.ignoreTasks(listOf(task!! to scope!!), apiContext)
                        setUIState(UIState.LOADING)
                    }
                }
            }
            R.id.tasks_context_item_unignore -> {
                if (task != null && scope != null) {
                    loginViewModel.apiContext.value?.also { apiContext ->
                        tasksViewModel.unignoreTasks(listOf(task!! to scope!!), apiContext)
                        setUIState(UIState.LOADING)
                    }
                }
            }
            R.id.tasks_context_item_import_in_calendar -> {
                if (task != null) {
                    startActivity(CalendarUtil.importTaskIntoCalendar(task!!))
                }
            }
            R.id.tasks_context_item_edit -> {
                if (task != null && scope != null) {
                    val action = ReadTaskFragmentDirections.actionReadTaskFragmentToEditTaskFragment(task!!.id, scope!!.login)
                    navController.navigate(action)
                }
            }
            R.id.tasks_context_item_delete -> {
                if (task != null && scope != null) {
                    val apiContext = loginViewModel.apiContext.value ?: return false
                    tasksViewModel.deleteTask(task!!, scope!!, apiContext)
                    setUIState(UIState.LOADING)
                }
            }
            else -> return false
        }
        return true
    }

    override fun onPrepareMenu(menu: Menu) {
        if (task != null && scope != null) {
            loginViewModel.apiContext.value?.also { apiContext ->
                val ignored = tasksViewModel.getIgnoredTasksBlocking(apiContext).any { it.id == task!!.id && it.scope == scope!!.login }
                menu.findItem(R.id.tasks_context_item_ignore).isVisible = !ignored
                menu.findItem(R.id.tasks_context_item_unignore).isVisible = ignored
            }
        }
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.fabEditTask.isEnabled = newState == UIState.READY
    }
}