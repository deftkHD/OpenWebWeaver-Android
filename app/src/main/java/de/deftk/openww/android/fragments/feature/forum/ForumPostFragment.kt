package de.deftk.openww.android.fragments.feature.forum

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ForumPostCommentAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentForumPostBinding
import de.deftk.openww.android.feature.forum.ForumPostIcons
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.ForumViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.forum.IForumPost
import java.text.DateFormat

class ForumPostFragment : AbstractFragment(true) {

    private val args: ForumPostFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val forumViewModel: ForumViewModel by activityViewModels()
    private val navController: NavController by lazy { findNavController() }

    private lateinit var binding: FragmentForumPostBinding
    private lateinit var group: IGroup
    private lateinit var post: IForumPost

    private var parent: IForumPost? = null
    private var deleted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentForumPostBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        setHasOptionsMenu(true)
        registerForContextMenu(binding.forumPostCommentList)
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

        forumViewModel.getAllForumPosts(group).observe(viewLifecycleOwner) { response ->
            enableUI(true)
            if (deleted)
                return@observe

            if (response is Response.Success) {
                parent = forumViewModel.getParentPost(response.value, (args.parentPostIds ?: emptyArray()).toMutableList())
                val post = forumViewModel.findPostOrComment(response.value, args.postId)
                if (post != null) {
                    this.post = post
                    val comments = forumViewModel.getComments(group, post.id)

                    binding.forumPostImage.setImageResource(ForumPostIcons.getByTypeOrDefault(post.icon).resource)
                    binding.forumPostTitle.text = post.title
                    binding.forumPostAuthor.text = post.created.member.name
                    binding.forumPostDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(post.created.date)
                    binding.forumPostText.text = TextUtils.parseMultipleQuotes(TextUtils.parseInternalReferences(TextUtils.parseHtml(post.text), group.login, navController))
                    binding.forumPostText.movementMethod = LinkMovementMethod.getInstance()
                    binding.forumPostText.transformationMethod = CustomTabTransformationMethod(binding.forumPostText.autoLinkMask)

                    binding.forumPostNoComments.isVisible = comments.isEmpty()
                    binding.forumPostCommentList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

                    val adapter = ForumPostCommentAdapter(group, arrayOf(*args.parentPostIds ?: emptyArray(), args.postId), forumViewModel, navController)
                    binding.forumPostCommentList.adapter = adapter
                    adapter.submitList(comments.sortedBy { it.created.date.time })

                } else {
                    Reporter.reportException(R.string.error_post_not_found, args.postId, requireContext())
                    navController.popBackStack()
                    return@observe
                }
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_posts_failed, response.exception, requireContext())
                navController.popBackStack()
                return@observe
            }
        }

        forumViewModel.deleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                forumViewModel.resetDeleteResponse() // mark as handled
            enableUI(true)

            if (response is Response.Success) {
                if (response.value == post) {
                    // self deleted
                    deleted = true
                    navController.popBackStack()
                } else {
                    // comment deleted
                    val comments = forumViewModel.getComments(group, post.id)
                    binding.forumPostNoComments.isVisible = comments.isEmpty()
                    binding.forumPostCommentList.isVisible = comments.isNotEmpty()
                    (binding.forumPostCommentList.adapter as ForumPostCommentAdapter).submitList(comments)
                }
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                //TODO implement
            } else {
                navController.popBackStack(R.id.forumPostsFragment, false)
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (group.effectiveRights.contains(Permission.FORUM_WRITE) || group.effectiveRights.contains(Permission.FORUM_ADMIN)) {
            inflater.inflate(R.menu.forum_post_options_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_delete -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    forumViewModel.deletePost(post, parent, group, apiContext)
                    enableUI(false)
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (group.effectiveRights.contains(Permission.FORUM_WRITE) || group.effectiveRights.contains(Permission.FORUM_ADMIN)) {
            requireActivity().menuInflater.inflate(R.menu.forum_post_options_menu, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.forumPostCommentList.adapter as ForumPostCommentAdapter
        when (item.itemId) {
            R.id.menu_item_delete -> {
                val comment = adapter.getItem(menuInfo.position)
                userViewModel.apiContext.value?.also { apiContext ->
                    forumViewModel.deletePost(comment, post, group, apiContext)
                    enableUI(false)
                }
            }
            else -> super.onContextItemSelected(item)
        }
        return true
    }

    override fun onUIStateChanged(enabled: Boolean) {
        binding.forumPostCommentList.isEnabled = enabled
    }
}