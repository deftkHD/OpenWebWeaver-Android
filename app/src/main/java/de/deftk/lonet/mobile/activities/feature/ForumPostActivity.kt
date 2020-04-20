package de.deftk.lonet.mobile.activities.feature

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.adapter.ForumPostAdapter
import kotlinx.android.synthetic.main.activity_forum_post.*
import java.text.DateFormat

class ForumPostActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LOGIN = "de.deftk.lonet.mobile.forum.forum"
        const val EXTRA_POST_ID = "de.deftk.lonet.mobile.forum.post_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forum_post)

        // back button in toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val member = AuthStore.getMember(intent.getStringExtra(EXTRA_LOGIN)!!)
        val postId = intent.getStringExtra(EXTRA_POST_ID)
        val post = member.getForumPosts(AuthStore.appUser.sessionId).first { it.id == postId }

        forum_post_image.setImageResource(ForumPostAdapter.postIconMap[post.icon] ?: R.drawable.ic_forum_post_unknown)
        forum_post_title.text = post.title
        forum_post_author.text = post.creationMember.name ?: post.creationMember.login
        forum_post_date.text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(post.creationDate)
        forum_post_text.text = post.text
    }

    // back button in toolbar functionality
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}
