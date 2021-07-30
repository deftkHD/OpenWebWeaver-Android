package de.deftk.openww.android.fragments.feature.tasks

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentReadTaskBinding
import de.deftk.openww.android.utils.CalendarUtil
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.TasksViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.tasks.ITask
import java.text.DateFormat

class ReadTaskFragment : Fragment() {

    private val args: ReadTaskFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val tasksViewModel: TasksViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentReadTaskBinding
    private lateinit var task: ITask
    private lateinit var scope: IOperatingScope

    private var deleted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadTaskBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()

        tasksViewModel.allTasksResponse.observe(viewLifecycleOwner) { response ->
            if (deleted)
                return@observe

            if (response is Response.Success) {
                val foundTask = response.value.firstOrNull { it.first.id == args.taskId && it.second.login == args.groupId }
                if (foundTask == null) {
                    Reporter.reportException(R.string.error_task_not_found, args.taskId, requireContext())
                    navController.popBackStack()
                    return@observe
                }

                task = foundTask.first
                scope = foundTask.second

                binding.taskTitle.text = task.title
                binding.taskAuthor.text = task.created.member.name
                binding.taskGroup.text = scope.name
                binding.taskCreated.text = String.format(getString(R.string.created_date), DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(task.created.date))
                binding.taskDue.text = String.format(getString(R.string.until_date), if (task.dueDate != null) DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(task.dueDate!!) else getString(R.string.not_set))
                binding.taskDetail.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(task.description), scope.login, navController)
                binding.taskDetail.movementMethod = LinkMovementMethod.getInstance()
                binding.taskDetail.transformationMethod = CustomTabTransformationMethod(binding.taskDetail.autoLinkMask)

                binding.fabEditTask.isVisible = scope.effectiveRights.contains(Permission.TASKS_WRITE) || scope.effectiveRights.contains(Permission.TASKS_ADMIN)
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_tasks_failed, response.exception, requireContext())
                navController.popBackStack()
                return@observe
            }
        }
        tasksViewModel.postResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                tasksViewModel.resetPostResponse() // mark as handled

            if (response is Response.Success) {
                deleted = true
                navController.popBackStack()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }
        binding.fabEditTask.setOnClickListener {
            val action = ReadTaskFragmentDirections.actionReadTaskFragmentToEditTaskFragment(task.id, scope.login, getString(R.string.edit_task))
            navController.navigate(action)
        }
        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (apiContext.user.getGroups().none { Feature.TASKS.isAvailable(it.effectiveRights) }) {
                    Toast.makeText(requireContext(), R.string.feature_not_available, Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                    return@observe
                }
                tasksViewModel.loadTasks(true, apiContext)
            } else {
                binding.taskTitle.text = ""
                binding.taskAuthor.text = ""
                binding.taskCreated.text = ""
                binding.taskDetail.text = ""
                binding.taskDue.text = ""
                binding.taskGroup.text = ""
                binding.fabEditTask.isVisible = false
            }
        }

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (scope.effectiveRights.contains(Permission.TASKS_WRITE) || scope.effectiveRights.contains(Permission.TASKS_ADMIN))
            inflater.inflate(R.menu.simple_edit_item_menu, menu)
        inflater.inflate(R.menu.task_item_menu, menu)

    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        userViewModel.apiContext.value?.also { apiContext ->
            val ignored = tasksViewModel.getIgnoredTasksBlocking(apiContext).any { it.id == task.id && it.scope == scope.login }
            menu.findItem(R.id.menu_item_ignore).isVisible = !ignored
            menu.findItem(R.id.menu_item_unignore).isVisible = ignored
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_ignore -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    tasksViewModel.ignoreTasks(listOf(task to scope), apiContext)
                }
            }
            R.id.menu_item_unignore -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    tasksViewModel.unignoreTasks(listOf(task to scope), apiContext)
                }
            }
            R.id.menu_item_import_in_calendar -> {
                startActivity(CalendarUtil.importTaskIntoCalendar(task))
            }
            R.id.menu_item_edit -> {
                val action = ReadTaskFragmentDirections.actionReadTaskFragmentToEditTaskFragment(task.id, scope.login, getString(R.string.edit_task))
                navController.navigate(action)
            }
            R.id.menu_item_delete -> {
                val apiContext = userViewModel.apiContext.value ?: return false
                tasksViewModel.deleteTask(task, scope, apiContext)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

}