package de.deftk.openlonet.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.daimajia.swipe.SwipeLayout
import de.deftk.lonet.api.model.feature.mailbox.Email
import de.deftk.lonet.api.model.feature.mailbox.EmailFolder
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.utils.SwipeAdapter
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat


class MailAdapter(context: Context, elements: List<Email>): FilterableAdapter<Email>(context, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = (convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_mail, parent, false)) as SwipeLayout
        val item = getItem(position) ?: return listItemView

        val swp = object : SwipeAdapter() {
            override fun onOpen(layout: SwipeLayout) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (item.folder.type == EmailFolder.EmailFolderType.TRASH) {
                            item.delete()
                        } else {
                            val trash = AuthStore.appUser.getEmailFolders()
                                .firstOrNull { it.type == EmailFolder.EmailFolderType.TRASH }
                            if (trash != null) {
                                item.move(trash)
                            } else {
                                item.delete()
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
        subjectView.text = item.subject
        if (item.unread != true)
            subjectView.setTypeface(null, Typeface.NORMAL)
        else
            subjectView.setTypeface(null, Typeface.BOLD)
        listItemView.findViewById<TextView>(R.id.mail_author).text = item.from
            ?.joinToString { it.name } ?: AuthStore.appUser.getName() //TODO not verified if this is useful
        listItemView.findViewById<TextView>(R.id.mail_date).text = DateFormat
            .getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(item.date)

        return listItemView
    }

    override fun search(constraint: String?): List<Email> {
        if (constraint == null)
            return originalElements
        return originalElements.filter {
                mail -> mail.subject.filterApplies(constraint) ||
                mail.from?.any { it.filterApplies(constraint) } == true
        }
    }

    override fun sort(elements: List<Email>): List<Email> {
        return elements.sortedByDescending { it.date }
    }
}