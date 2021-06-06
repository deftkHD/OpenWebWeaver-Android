package de.deftk.openlonet.activities.feature.forum

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import de.deftk.lonet.api.implementation.Group
import de.deftk.lonet.api.implementation.feature.forum.ForumPost
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.ForumPostAdapter
import de.deftk.openlonet.adapter.ForumPostCommentRecyclerAdapter
import de.deftk.openlonet.databinding.ActivityForumPostBinding
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.getJsonExtra
import java.text.DateFormat

class ForumPostActivity : AppCompatActivity() {

    //TODO allow swipe refresh (reload comments)

    companion object {
        const val EXTRA_FORUM_POST = "de.deftk.openlonet.forum.forum_post_extra"
        const val EXTRA_GROUP = "de.deftk.openlonet.forum.group_extra"
    }

    private lateinit var binding: ActivityForumPostBinding

    private lateinit var post: ForumPost
    private lateinit var group: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForumPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // back button in toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.see_post)

        if (intent.hasExtra(EXTRA_FORUM_POST) && intent.hasExtra(EXTRA_GROUP)) {
            post = intent.getJsonExtra(EXTRA_FORUM_POST)!!
            group = intent.getJsonExtra(EXTRA_GROUP)!!

            binding.forumPostImage.setImageResource(ForumPostAdapter.postIconMap[post.icon] ?: R.drawable.ic_help_24)
            binding.forumPostTitle.text = post.title
            binding.forumPostAuthor.text = post.created.member.name
            binding.forumPostDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(post.created.date)
            binding.forumPostText.text = TextUtils.parseMultipleQuotes(TextUtils.parseInternalReferences(TextUtils.parseHtml(post.text)))
            binding.forumPostText.movementMethod = LinkMovementMethod.getInstance()
            binding.forumPostText.transformationMethod = CustomTabTransformationMethod(binding.forumPostText.autoLinkMask)

            binding.forumPostNoComments.isVisible = post.getComments().isEmpty()
            binding.forumPostCommentRecyclerView.layoutManager = LinearLayoutManager(this)
            //binding.forumPostCommentRecyclerView.adapter = ForumPostCommentRecyclerAdapter(post.getComments().sortedBy { it.created.date.time }, group)
        } else {
            finish()
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}
