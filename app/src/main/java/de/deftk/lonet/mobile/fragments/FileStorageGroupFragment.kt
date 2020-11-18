package de.deftk.lonet.mobile.fragments

import android.content.Intent
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.Group
import de.deftk.lonet.api.model.User
import de.deftk.lonet.api.model.abstract.AbstractOperator
import de.deftk.lonet.api.model.feature.Quota
import de.deftk.lonet.api.model.feature.abstract.IFileStorage
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.IBackHandler
import de.deftk.lonet.mobile.activities.feature.FilesActivity
import de.deftk.lonet.mobile.adapter.FileStorageAdapter
import de.deftk.lonet.mobile.feature.AppFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileStorageGroupFragment : GroupFragment(
    AppFeature.FEATURE_FILE_STORAGE,
    R.layout.fragment_file_storage,
    R.id.file_list,
    R.id.file_storage_swipe_refresh,
    R.id.progress_file_storage,
    R.id.file_empty
), IBackHandler {

    companion object {
        const val ARGUMENT_GROUP = "de.deftk.lonet.mobile.files.argument_group"
        const val ARGUMENT_FILE_ID = "de.deftk.lonet.mobile.files.argument_file_id"
    }

    override fun createAdapter(groups: List<AbstractOperator>): ArrayAdapter<*> {
        throw IllegalStateException("Not implemented")
    }

    override suspend fun loadGroups() {
        try {
            val groups = mutableListOf<AbstractOperator>()
            groups.addAll(AuthStore.appUser.getContext().getGroups()
                .filter { shouldGroupBeShown(it) })
            if (shouldUserBeShown(AuthStore.appUser))
                groups.add(0, AuthStore.appUser)
            //TODO inefficient to query file storage quota for every file storage; should only be one big request
            val groupData = groups.map {
                Pair(
                    it as IFileStorage,
                    try {
                        it.getFileStorageState().second
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Quota(0, 0, 0, 0, -1, -1)
                    }
                )
            }.toMap()
            withContext(Dispatchers.Main) {
                groupList.adapter = FileStorageAdapter(requireContext(), groupData)
                emptyLabel.isVisible = groups.isEmpty()
                swipeRefreshLayout.isRefreshing = false
                progressBar.visibility = ProgressBar.INVISIBLE
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                swipeRefreshLayout.isRefreshing = false
                progressBar.visibility = ProgressBar.INVISIBLE
                Toast.makeText(
                    context,
                    getString(R.string.request_failed_other).format(e.message ?: e),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun shouldGroupBeShown(group: Group): Boolean {
        return Feature.FILES.isAvailable(group.effectiveRights)
    }

    override fun shouldUserBeShown(user: User): Boolean {
        return Feature.FILES.isAvailable(user.effectiveRights)
    }

    override fun onItemClick(operator: AbstractOperator) {
        val intent = Intent(context, FilesActivity::class.java)
        intent.putExtra(FilesActivity.EXTRA_FOLDER, operator)
        startActivity(intent)
    }

    /*override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (arguments != null) {
            val group = arguments?.getString(ARGUMENT_GROUP)
            val file = arguments?.getString(ARGUMENT_FILE_ID)
            //TODO jump to given location
        }

        val view = inflater.inflate(R.layout.fragment_file_storage, container, false)
        val list = view.findViewById<ListView>(R.id.file_list)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.file_storage_swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            list.adapter = null
            CoroutineScope(Dispatchers.IO).launch {
                if (history.size == 0) {
                    loadFiles(null)
                } else {
                    loadFiles(history.peek())
                }
            }
        }
        list.setOnItemClickListener { _, _, position, _ ->
            when (val item = list.getItemAtPosition(position)) {
                is OnlineFile -> {
                    if (item.type == OnlineFile.FileType.FOLDER) {
                        navigate(item)
                    } else {
                        CoroutineScope(Dispatchers.IO).launch {
                            downloadFile(item)
                        }
                        Toast.makeText(context, getString(R.string.download_started), Toast.LENGTH_SHORT).show()
                    }
                }
                is IFileStorage -> {
                    navigate(item)
                }
            }

        }
        list.setOnItemLongClickListener { _, _, position, _ ->
            val item = list.getItemAtPosition(position)
            //TODO show context menu
            true
        }

        navigate(if (history.isNotEmpty()) history.pop() else currentFileStorage)
        return view
    }

    private fun navigate(directory: Any?) {
        file_list?.adapter = null
        progress_file_storage?.visibility = ProgressBar.VISIBLE
        when (directory) {
            is IFilePrimitive -> {
                if (directory is IFileStorage)
                    currentFileStorage = directory
                else
                    history.push(directory)
                CoroutineScope(Dispatchers.IO).launch {
                    loadFiles(directory)
                }
            }
            null -> {
                history.clear()
                currentFileStorage = null
                CoroutineScope(Dispatchers.IO).launch {
                    loadFiles(null)
                }
            }
        }
    }

    private suspend fun loadFolders() {
        val groups = (AuthStore.appUser.getContext().getGroups().filter { Feature.FILES.isAvailable(it.effectiveRights) } as List<IFileStorage>).sortedBy { (it as AbstractOperator).getName() }.toMutableList().apply { this.add(0, AuthStore.appUser) }.map {
            try {
                Pair(it, it.getFileStorageState().second)
            } catch (e: Exception) {
                Pair(it, Quota(0, 0, 0, 0, -1, -1))
            }
        }
        withContext(Dispatchers.Main) {
            file_list?.adapter = FileStorageAdapter(requireContext(), groups.toMap())
            file_empty.isVisible = groups.isEmpty()
            progress_file_storage?.visibility = ProgressBar.INVISIBLE
            file_storage_swipe_refresh?.isRefreshing = false
        }
    }*/

    override fun getTitle(): String {
        return getString(R.string.file_storage)
    }

}