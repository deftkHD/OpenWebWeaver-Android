package de.deftk.openww.android.fragments.feature.tasks

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.tasks.ITask
import de.deftk.openww.android.R
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentReadTaskBinding
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.TasksViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import java.text.DateFormat

class ReadTaskFragment : Fragment() {

    private val args: ReadTaskFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val tasksViewModel: TasksViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentReadTaskBinding
    private lateinit var task: ITask
    private lateinit var scope: IOperatingScope

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReadTaskBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tasksViewModel.tasksResponse.observe(viewLifecycleOwner) { resource ->
            if (resource is Response.Success) {
                resource.value.firstOrNull { it.first.id == args.taskId && it.second.login == args.groupId }?.apply {
                    task = first
                    scope = second

                    binding.taskTitle.text = task.getTitle()
                    binding.taskAuthor.text = task.created.member.name
                    binding.taskGroup.text = scope.name
                    binding.taskCreated.text = String.format(getString(R.string.created_date), DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(task.created.date))
                    binding.taskDue.text = String.format(getString(R.string.until_date), if (task.getEndDate() != null) DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(task.getEndDate()!!) else getString(R.string.not_set))
                    binding.taskDetail.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(task.getDescription()))
                    binding.taskDetail.movementMethod = LinkMovementMethod.getInstance()
                    binding.taskDetail.transformationMethod = CustomTabTransformationMethod(binding.taskDetail.autoLinkMask)

                    if (scope.effectiveRights.contains(Permission.TASKS_ADMIN)) {
                        binding.fabEditTask.isVisible = true
                        binding.fabEditTask.setOnClickListener {
                            val action = ReadTaskFragmentDirections.actionReadTaskFragmentToEditTaskFragment(task.id, scope.login, getString(R.string.edit_task))
                            navController.navigate(action)
                        }
                    }
                }
            } else if (resource is Response.Failure) {
                resource.exception.printStackTrace()
                //TODO handle error
            }
        }
        tasksViewModel.postResponse.observe(viewLifecycleOwner) { result ->
            if (result != null)
                tasksViewModel.resetPostResponse() // mark as handled

            if (result is Response.Success) {
                navController.popBackStack()
            } else if (result is Response.Failure) {
                //TODO handle error
                result.exception.printStackTrace()
            }
        }
        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext == null) {
                navController.popBackStack(R.id.tasksFragment, false)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (scope.effectiveRights.contains(Permission.TASKS_ADMIN))
            inflater.inflate(R.menu.simple_edit_item_menu, menu)
        inflater.inflate(R.menu.read_task_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_import_in_calendar -> {
                val intent = Intent(Intent.ACTION_INSERT)
                intent.data = CalendarContract.Events.CONTENT_URI
                intent.putExtra(CalendarContract.Events.TITLE, task.getTitle())
                if (task.getStartDate() != null)
                    intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, task.getStartDate()!!.time)
                if (task.getEndDate() != null)
                    intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, task.getEndDate()!!.time)
                if (task.getDescription() != null)
                    intent.putExtra(CalendarContract.Events.DESCRIPTION, task.getDescription())
                startActivity(intent)
                true
            }
            R.id.menu_item_edit -> {
                val action = ReadTaskFragmentDirections.actionReadTaskFragmentToEditTaskFragment(task.id, scope.login, getString(R.string.edit_task))
                navController.navigate(action)
                true
            }
            R.id.menu_item_delete -> {
                val apiContext = userViewModel.apiContext.value ?: return false
                tasksViewModel.deleteTask(task, scope, apiContext)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}