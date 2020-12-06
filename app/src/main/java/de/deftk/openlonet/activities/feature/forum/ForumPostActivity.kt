package de.deftk.openlonet.activities.feature.forum

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import de.deftk.lonet.api.model.feature.forum.ForumPost
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.ForumPostAdapter
import de.deftk.openlonet.adapter.ForumPostCommentRecyclerAdapter
import de.deftk.openlonet.databinding.ActivityForumPostBinding
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
import java.text.DateFormat

class ForumPostActivity : AppCompatActivity() {

    //TODO allow swipe refresh (reload comments)

    companion object {
        const val EXTRA_FORUM_POST = "de.deftk.openlonet.forum.forum_post_extra"
    }

    private lateinit var binding: ActivityForumPostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForumPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // back button in toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.see_post)


        val post = intent.getSerializableExtra(EXTRA_FORUM_POST) as? ForumPost

        if (post != null) {
            binding.forumPostImage.setImageResource(ForumPostAdapter.postIconMap[post.icon] ?: R.drawable.ic_help_24)
            binding.forumPostTitle.text = post.title
            binding.forumPostAuthor.text = post.creationMember.getName()
            binding.forumPostDate.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(post.creationDate)
            binding.forumPostText.text = TextUtils.parseMultipleQuotes(TextUtils.parseInternalReferences(TextUtils.parseHtml(post.text)))
            binding.forumPostText.movementMethod = LinkMovementMethod.getInstance()
            binding.forumPostText.transformationMethod = CustomTabTransformationMethod(binding.forumPostText.autoLinkMask)

            binding.forumPostNoComments.isVisible = post.commentCount <= 0
            binding.forumPostCommentRecyclerView.layoutManager = LinearLayoutManager(this)
            binding.forumPostCommentRecyclerView.adapter = ForumPostCommentRecyclerAdapter(post.comments.sortedBy { it.creationDate.time })
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}
