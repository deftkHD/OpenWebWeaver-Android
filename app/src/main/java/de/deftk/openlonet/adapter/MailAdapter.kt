package de.deftk.openlonet.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.daimajia.swipe.SwipeLayout
import de.deftk.lonet.api.implementation.feature.mailbox.Email
import de.deftk.lonet.api.implementation.feature.mailbox.EmailFolder
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.utils.SwipeAdapter
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MailAdapter(context: Context, elements: List<Email>, private val folder: EmailFolder): FilterableAdapter<Email>(context, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = (convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_mail, parent, false)) as SwipeLayout
        val item = getItem(position) ?: return listItemView

        val swp = object : SwipeAdapter() {
            override fun onOpen(layout: SwipeLayout) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (folder.isTrash()) {
                            item.delete(folder, AuthStore.getUserContext())
                        } else {
                            val trash = AuthStore.getApiUser().getEmailFolders(AuthStore.getUserContext())
                                .firstOrNull { it.isTrash() }
                            if (trash != null) {
                                item.move(folder, trash, AuthStore.getUserContext())
                            } else {
                                item.delete(folder, AuthStore.getUserContext())
                            }
                        }
                        withContext(Dispatchers.Main) {
                            remove(item)
                            notifyDataSetChanged()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.error).format(e.message ?: e),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
        // clear all swipe listeners; there should only be one. sadly there seems to be no official
        // way to do this
        (listItemView::class.java.getDeclaredField("mSwipeListeners")
            .apply { this.isAccessible = true }.get(listItemView) as ArrayList<*>).clear()

        listItemView.addSwipeListener(swp)

        val subjectView = listItemView.findViewById<TextView>(R.id.mail_subject)
        subjectView.text = item.getSubject()
        if (!item.isUnread())
            subjectView.setTypeface(null, Typeface.NORMAL)
        else
            subjectView.setTypeface(null, Typeface.BOLD)
        listItemView.findViewById<TextView>(R.id.mail_author).text = item.getFrom()
            ?.joinToString { it.name } ?: AuthStore.getApiUser().name //TODO not verified if this is useful
        listItemView.findViewById<TextView>(R.id.mail_date).text = TextUtils.parseShortDate(item.getDate())

        listItemView.findViewById<ImageView>(R.id.mail_answered).visibility =
            if (item.isAnswered()) View.VISIBLE else View.INVISIBLE
        listItemView.findViewById<ImageView>(R.id.mail_flagged).visibility =
            if (item.isFlagged()) View.VISIBLE else View.INVISIBLE

        return listItemView
    }

    override fun search(constraint: String?): List<Email> {
        if (constraint == null)
            return originalElements
        return originalElements.filter {
                mail -> mail.getSubject().filterApplies(constraint) ||
                mail.getFrom()?.any { it.filterApplies(constraint) } == true
        }
    }

    override fun sort(elements: List<Email>): List<Email> {
        return elements.sortedByDescending { it.getDate() }
    }
}