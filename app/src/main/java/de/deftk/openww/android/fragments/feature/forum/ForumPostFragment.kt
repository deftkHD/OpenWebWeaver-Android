package de.deftk.openww.android.fragments.feature.forum

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ForumPostCommentAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentForumPostBinding
import de.deftk.openww.android.feature.forum.ForumPostIcons
import de.deftk.openww.android.fragments.ContextualFragment
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.viewmodel.ForumViewModel
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.forum.IForumPost
import java.text.DateFormat

class ForumPostFragment : ContextualFragment(true) {

    private val args: ForumPostFragmentArgs by navArgs()
    private val forumViewModel: ForumViewModel by activityViewModels()

    private lateinit var binding: FragmentForumPostBinding
    private lateinit var post: IForumPost

    private var parent: IForumPost? = null
    private var group: IGroup? = null
    private var deleted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentForumPostBinding.inflate(inflater, container, false)

        val adapter by lazy { ForumPostCommentAdapter(group!!, arrayOf(*args.parentPostIds ?: emptyArray(), args.postId), forumViewModel, navController) }

        forumViewModel.deleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                forumViewModel.resetDeleteResponse() // mark as handled

            if (response is Response.Success) {
                setUIState(UIState.READY)
                if (response.value == post) {
                    // self deleted
                    deleted = true
                    navController.popBackStack()
                } else {
                    // comment deleted
                    val comments = forumViewModel.getComments(group!!, post.id)
                    binding.forumPostNoComments.isVisible = comments.isEmpty()
                    binding.forumPostCommentList.isVisible = comments.isNotEmpty()
                    (binding.forumPostCommentList.adapter as ForumPostCommentAdapter).submitList(comments)
                }
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }

        loginViewModel.apiContext.observe(viewLifecycleOwner) apiContext@ { apiContext ->
            if (apiContext != null) {
                val group = apiContext.user.getGroups().firstOrNull { it.login == args.groupId }
                if (group == null) {
                    Reporter.reportException(R.string.error_scope_not_found, args.groupId, requireContext())
                    navController.popBackStack()
                    return@apiContext
                }
                if (!Feature.FORUM.isAvailable(group.effectiveRights)) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@apiContext
                }
                if (this.group != null) {
                    forumViewModel.getFilteredForumPosts(this.group!!).removeObservers(viewLifecycleOwner)
                    this.group = group
                    adapter.group = group
                } else {
                    this.group = group
                }
                binding.forumPostCommentList.adapter = adapter
                binding.forumPostCommentList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

                forumViewModel.getAllForumPosts(group).observe(viewLifecycleOwner) posts@ { response ->
                    if (deleted)
                        return@posts

                    if (response is Response.Success) {
                        parent = forumViewModel.getParentPost(response.value, (args.parentPostIds ?: emptyArray()).toMutableList())
                        val post = forumViewModel.findPostOrComment(response.value, args.postId)
                        if (post != null) {
                            this.post = post
                            val comments = forumViewModel.getComments(group, post.id)

                            binding.forumPostImage.setImageResource(ForumPostIcons.getByTypeOrDefault(post.icon).resource)
                            binding.forumPostTitle.text = post.title
                            binding.forumPostAuthor.text = post.created.member.name
                            binding.forumPostDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(post.created.date!!)
                            binding.forumPostText.text = TextUtils.parseMultipleQuotes(TextUtils.parseInternalReferences(TextUtils.parseHtml(post.text), group.login, navController))
                            binding.forumPostText.movementMethod = LinkMovementMethod.getInstance()
                            binding.forumPostText.transformationMethod = CustomTabTransformationMethod(binding.forumPostText.autoLinkMask)

                            binding.forumPostNoComments.isVisible = comments.isEmpty()
                            binding.forumPostCommentList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

                            binding.forumPostCommentList.adapter = adapter
                            adapter.submitList(comments.sortedBy { it.created.date?.time })
                            binding.forumPostNoComments.isVisible = comments.isEmpty()

                            setUIState(UIState.READY)
                        } else {
                            setUIState(UIState.ERROR)
                            Reporter.reportException(R.string.error_post_not_found, args.postId, requireContext())
                            navController.popBackStack()
                            return@posts
                        }
                    } else if (response is Response.Failure) {
                        setUIState(UIState.ERROR)
                        Reporter.reportException(R.string.error_get_posts_failed, response.exception, requireContext())
                        navController.popBackStack()
                        return@posts
                    }
                }

                if (forumViewModel.getAllForumPosts(group).value == null) {
                    forumViewModel.loadForumPosts(group, null, apiContext)
                    setUIState(UIState.LOADING)
                }
            } else {
                adapter.submitList(emptyList())
                setUIState(UIState.DISABLED)
            }
        }
        registerForContextMenu(binding.forumPostCommentList)
        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (group!!.effectiveRights.contains(Permission.FORUM_WRITE) || group!!.effectiveRights.contains(Permission.FORUM_ADMIN)) {
            menuInflater.inflate(R.menu.forum_context_menu, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.forum_context_item_delete -> {
                loginViewModel.apiContext.value?.also { apiContext ->
                    forumViewModel.deletePost(post, parent, group!!, apiContext)
                    setUIState(UIState.LOADING)
                }
            }
            else -> return false
        }
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (group!!.effectiveRights.contains(Permission.FORUM_WRITE) || group!!.effectiveRights.contains(Permission.FORUM_ADMIN)) {
            requireActivity().menuInflater.inflate(R.menu.forum_context_menu, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.forumPostCommentList.adapter as ForumPostCommentAdapter
        when (item.itemId) {
            R.id.forum_context_item_delete -> {
                val comment = adapter.getItem(menuInfo.position)
                loginViewModel.apiContext.value?.also { apiContext ->
                    forumViewModel.deletePost(comment, post, group!!, apiContext)
                    setUIState(UIState.LOADING)
                }
            }
            else -> super.onContextItemSelected(item)
        }
        return true
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.forumPostCommentList.isEnabled = newState.listEnabled
    }
}