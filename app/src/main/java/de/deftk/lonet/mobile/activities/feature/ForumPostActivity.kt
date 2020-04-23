package de.deftk.lonet.mobile.activities.feature

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.feature.forum.ForumPost
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.adapter.ForumPostAdapter
import de.deftk.lonet.mobile.utils.TextUtils
import kotlinx.android.synthetic.main.activity_forum_post.*
import java.text.DateFormat

class ForumPostActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FORUM_POST = "de.deftk.lonet.mobile.forum.forum_post_extra"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forum_post)

        // back button in toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.see_post)


        val post = intent.getSerializableExtra(EXTRA_FORUM_POST) as? ForumPost

        if (post != null) {
            forum_post_image.setImageResource(ForumPostAdapter.postIconMap[post.icon] ?: R.drawable.ic_forum_post_unknown)
            forum_post_title.text = post.title
            forum_post_author.text = post.creationMember.getName()
            forum_post_date.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(post.creationDate)
            forum_post_text.text = TextUtils.parseInternalReferences(TextUtils.parseHtml(post.text))
            forum_post_text.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}
