package de.deftk.openlonet.fragments.feature.forum

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.lonet.api.model.Feature
import de.deftk.openlonet.adapter.recycler.ForumGroupAdapter
import de.deftk.openlonet.databinding.FragmentForumBinding
import de.deftk.openlonet.viewmodel.UserViewModel

class ForumGroupFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var binding: FragmentForumBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentForumBinding.inflate(inflater, container, false)

        val adapter = ForumGroupAdapter()
        binding.forumList.adapter = adapter
        binding.forumList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.forumSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                updateGroups(adapter, apiContext)
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                updateGroups(adapter, apiContext)
            } else {
                binding.forumEmpty.isVisible = false
                adapter.submitList(emptyList())
                binding.forumEmpty.isVisible = true
            }
        }

        return binding.root
    }

    private fun updateGroups(adapter: ForumGroupAdapter, apiContext: ApiContext) {
        val groups = apiContext.getUser().getGroups().filter { Feature.MEMBERS.isAvailable(it.effectiveRights) }
        adapter.submitList(groups)
        binding.forumEmpty.isVisible = groups.isEmpty()
        binding.progressForum.isVisible = false
        binding.forumSwipeRefresh.isRefreshing = false
    }

}