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
import de.deftk.openlonet.utils.CustomTabTransformationMethod
import de.deftk.openlonet.utils.TextUtils
import kotlinx.android.synthetic.main.activity_forum_post.*
import java.text.DateFormat

class ForumPostActivity : AppCompatActivity() {

    //TODO allow swipe refresh (reload comments)

    companion object {
        const val EXTRA_FORUM_POST = "de.deftk.openlonet.forum.forum_post_extra"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forum_post)

        // back button in toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.see_post)


        val post = intent.getSerializableExtra(EXTRA_FORUM_POST) as? ForumPost

        if (post != null) {
            forum_post_image.setImageResource(ForumPostAdapter.postIconMap[post.icon] ?: R.drawable.ic_help_24)
            forum_post_title.text = post.title
            forum_post_author.text = post.creationMember.getName()
            forum_post_date.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(post.creationDate)
            forum_post_text.text = TextUtils.parseMultipleQuotes(TextUtils.parseInternalReferences(TextUtils.parseHtml(post.text)))
            forum_post_text.movementMethod = LinkMovementMethod.getInstance()
            forum_post_text.transformationMethod = CustomTabTransformationMethod(forum_post_text.autoLinkMask)

            forum_post_no_comments.isVisible = post.commentCount <= 0
            forum_post_comment_recycler_view.layoutManager = LinearLayoutManager(this)
            forum_post_comment_recycler_view.adapter = ForumPostCommentRecyclerAdapter(post.comments.sortedBy { it.creationDate.time })
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}
