package de.deftk.openlonet.fragments.feature.forum

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.lonet.api.model.IGroup
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.forum.IForumPost
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.ForumPostCommentRecyclerAdapter
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.databinding.FragmentForumPostBinding
import de.deftk.openlonet.feature.forum.ForumPostIcons
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.viewmodel.ForumViewModel
import de.deftk.openlonet.viewmodel.UserViewModel
import java.text.DateFormat

class ForumPostFragment : Fragment() {

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
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val group = userViewModel.apiContext.value?.getUser()?.getGroups()?.firstOrNull { it.login == args.groupId }
        if (group == null) {
            Log.e(TAG, "Failed to find group")
            return
        }
        this.group = group

        forumViewModel.getForumPosts(group).observe(viewLifecycleOwner) { resource ->
            if (resource is Response.Success) {
                val post = forumViewModel.findPostOrComment(resource.value, args.parentPostIds?.toMutableList(), args.postId)
                if (post != null) {
                    this.post = post

                    binding.forumPostImage.setImageResource(ForumPostIcons.getByApiColorOrDefault(post.icon).resource)
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
            } else if (resource is Response.Failure) {
                //TODO handle error
                resource.exception.printStackTrace()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (group.effectiveRights.contains(Permission.FORUM_ADMIN)) {
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