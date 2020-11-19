package de.deftk.openlonet.activities.feature.forum

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import de.deftk.lonet.api.model.feature.forum.ForumPost
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.ForumPostAdapter
import de.deftk.openlonet.utils.TextUtils
import kotlinx.android.synthetic.main.activity_forum_post.*
import java.text.DateFormat

class ForumPostActivity : AppCompatActivity() {

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
