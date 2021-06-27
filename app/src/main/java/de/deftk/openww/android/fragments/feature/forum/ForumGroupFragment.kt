package de.deftk.openww.android.fragments.feature.forum

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.databinding.FragmentForumBinding
import de.deftk.openww.android.fragments.AbstractGroupFragment
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IOperatingScope

class ForumGroupFragment : AbstractGroupFragment() {

    private lateinit var binding: FragmentForumBinding

    override val scopePredicate: (T: IOperatingScope) -> Boolean = { Feature.FORUM.isAvailable(it.effectiveRights) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentForumBinding.inflate(inflater, container, false)
        binding.forumList.adapter = adapter
        binding.forumList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        registerSwipeRefresh(binding.forumSwipeRefresh)
        return binding.root
    }

    override fun setUI(empty: Boolean, loading: Boolean, refreshing: Boolean) {
        binding.forumEmpty.isVisible = empty
        binding.progressForum.isVisible = loading
        binding.forumSwipeRefresh.isRefreshing = refreshing
    }

    override fun onOperatingScopeClicked(scope: IOperatingScope) {
        navController.navigate(ForumGroupFragmentDirections.actionForumGroupFragmentToForumPostsFragment(scope.login, scope.name))
    }

}