package de.deftk.openww.android.fragments.feature.forum

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.forum.IForumPost
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ForumPostCommentRecyclerAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentForumPostBinding
import de.deftk.openww.android.feature.forum.ForumPostIcons
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.ForumViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import java.text.DateFormat

class ForumPostFragment : Fragment() {

    //TODO options menu

    companion object {
        private val TAG = ForumPostFragment::class.java.simpleName
    }

    private val args: ForumPostFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val forumViewModel: ForumViewModel by activityViewModels()
    private val navController: NavController by lazy { findNavController() }

    private lateinit var binding: FragmentForumPostBinding
    private lateinit var group: IGroup
    private lateinit var post: IForumPost

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentForumPostBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val group = userViewModel.apiContext.value?.user?.getGroups()?.firstOrNull { it.login == args.groupId }
        if (group == null) {
            Reporter.reportException(R.string.error_scope_not_found, args.groupId, requireContext())
            navController.popBackStack()
            return
        }
        this.group = group

        forumViewModel.getForumPosts(group).observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                val post = forumViewModel.findPostOrComment(response.value, args.parentPostIds?.toMutableList(), args.postId)
                if (post != null) {
                    this.post = post

                    binding.forumPostImage.setImageResource(ForumPostIcons.getByTypeOrDefault(post.icon).resource)
                    binding.forumPostTitle.text = post.title
                    binding.forumPostAuthor.text = post.created.member.name
                    binding.forumPostDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(post.created.date)
                    binding.forumPostText.text = TextUtils.parseMultipleQuotes(TextUtils.parseInternalReferences(TextUtils.parseHtml(post.text)))
                    binding.forumPostText.movementMethod = LinkMovementMethod.getInstance()
                    binding.forumPostText.transformationMethod = CustomTabTransformationMethod(binding.forumPostText.autoLinkMask)

                    binding.forumPostNoComments.isVisible = post.getComments().isEmpty()
                    binding.forumPostCommentRecyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
                    binding.forumPostCommentRecyclerView.adapter = ForumPostCommentRecyclerAdapter(post.getComments().sortedBy { it.created.date.time }, group, navController, args.parentPostIds ?: emptyArray(), args.postId)
                }
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_posts_failed, response.exception, requireContext())
                navController.popBackStack()
                return@observe
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext == null) {
                navController.popBackStack(R.id.forumPostsFragment, false)
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (group.effectiveRights.contains(Permission.FORUM_WRITE) || group.effectiveRights.contains(Permission.FORUM_ADMIN)) {
            //inflater.inflate(R.menu.forum_post_options_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_delete -> {
                //TODO implement
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}