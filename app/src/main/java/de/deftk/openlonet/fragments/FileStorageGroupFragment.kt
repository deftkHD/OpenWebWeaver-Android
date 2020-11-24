package de.deftk.openlonet.fragments

import android.content.Intent
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.Group
import de.deftk.lonet.api.model.User
import de.deftk.lonet.api.model.abstract.AbstractOperator
import de.deftk.lonet.api.model.feature.Quota
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.abstract.IBackHandler
import de.deftk.openlonet.activities.feature.FilesActivity
import de.deftk.openlonet.adapter.FileStorageAdapter
import de.deftk.openlonet.feature.AppFeature
import de.deftk.openlonet.utils.filter.FilterableAdapter
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
        const val ARGUMENT_GROUP = "de.deftk.openlonet.files.argument_group"
        const val ARGUMENT_FILE_ID = "de.deftk.openlonet.files.argument_file_id"
    }

    override fun createAdapter(groups: List<AbstractOperator>): FilterableAdapter<*> {
        throw IllegalStateException("Not implemented")
    }

    override suspend fun loadGroups() {
        try {
            val groups = mutableListOf<AbstractOperator>()
            groups.addAll(
                AuthStore.getAppUser().getContext().getGroups()
                .filter { shouldGroupBeShown(it) })
            if (shouldUserBeShown(AuthStore.getAppUser()))
                groups.add(0, AuthStore.getAppUser())
            //TODO inefficient to query file storage quota for every file storage; should only be one big request
            val groupData = groups.map {
                Pair(
                    it,
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

    override fun getTitle(): String {
        return getString(R.string.file_storage)
    }

}