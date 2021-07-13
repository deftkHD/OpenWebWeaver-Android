package de.deftk.openww.android.fragments.feature.forum

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ForumPostAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentForumPostsBinding
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.ForumViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.IGroup

class ForumPostsFragment : Fragment() {

    //TODO context menu
    //TODO action mode

    private val args: ForumPostsFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val forumViewModel: ForumViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentForumPostsBinding
    private lateinit var group: IGroup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentForumPostsBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        val group = userViewModel.apiContext.value?.user?.getGroups()?.firstOrNull { it.login == args.groupId }
        if (group == null) {
            Reporter.reportException(R.string.error_scope_not_found, args.groupId, requireContext())
            navController.popBackStack(R.id.forumGroupFragment, false)
            return binding.root
        }
        this.group = group

        val adapter = ForumPostAdapter(group)
        binding.forumList.adapter = adapter
        binding.forumList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        forumViewModel.getForumPosts(group).observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                val posts = forumViewModel.filterRootPosts(response.value)
                adapter.submitList(posts)
                binding.forumEmpty.isVisible = posts.isEmpty()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_posts_failed, response.exception, requireContext())
            }
            binding.progressForum.isVisible = false
            binding.forumSwipeRefresh.isRefreshing = false
        }

        binding.forumSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                forumViewModel.loadForumPosts(group, null, apiContext)
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                val newGroup = userViewModel.apiContext.value?.user?.getGroups()?.firstOrNull { it.login == args.groupId }
                if (newGroup != null) {
                    forumViewModel.loadForumPosts(group, null, apiContext)
                } else {
                    navController.popBackStack(R.id.forumGroupFragment, false)
                }
            } else {
                binding.forumEmpty.isVisible = false
                adapter.submitList(emptyList())
                binding.progressForum.isVisible = true
            }
        }

        return binding.root
    }

    /*override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                //TODO search
                return false
            }
        })
    }*/

}