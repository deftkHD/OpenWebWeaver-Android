package de.deftk.openlonet.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import com.daimajia.swipe.SwipeLayout
import de.deftk.lonet.api.model.feature.mailbox.IEmail
import de.deftk.lonet.api.model.feature.mailbox.IEmailFolder
import de.deftk.openlonet.R
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.utils.SwipeAdapter
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies
import de.deftk.openlonet.viewmodel.MailboxViewModel
import de.deftk.openlonet.viewmodel.UserViewModel


class MailAdapter(
    context: Context,
    elements: List<IEmail>,
    private val folder: IEmailFolder,
    private val mailboxViewModel: MailboxViewModel,
    private val userViewModel: UserViewModel,
    private val viewLifecycleOwner: LifecycleOwner
) : FilterableAdapter<IEmail>(context, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = (convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_mail, parent, false)) as SwipeLayout
        val item = getItem(position) ?: return listItemView

        val swp = object : SwipeAdapter() {
            override fun onOpen(layout: SwipeLayout) {
                if (folder.isTrash) {
                    /*userViewModel.apiContext.value?.apply {
                        mailboxViewModel.deleteEmail(item, folder, this)
                            .observe(viewLifecycleOwner) { result ->
                                if (result is Response.Failure) {
                                    result.exception.printStackTrace()
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.error)
                                            .format(result.exception.message ?: result.exception),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    }*/
                } else {
                    /*val trash = mailboxViewModel.foldersResponse.value?.firstOrNull { it.isTrash }
                    if (trash != null) {
                        userViewModel.apiContext.value?.apply {
                            mailboxViewModel.moveEmail(item, folder, trash, this)
                                .observe(viewLifecycleOwner) { result ->
                                    if (result is Response.Failure) {
                                        result.exception.printStackTrace()
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.error).format(
                                                result.exception.message ?: result.exception
                                            ),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                        }
                    } else {
                        userViewModel.apiContext.value?.apply {
                            mailboxViewModel.deleteEmail(item, folder, this)
                                .observe(viewLifecycleOwner) { result ->
                                    if (result is Response.Failure) {
                                        result.exception.printStackTrace()
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.error).format(
                                                result.exception.message ?: result.exception
                                            ),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                        }
                    }*/
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
            ?.joinToString { it.name }
            ?: "UNKNOWN" //AuthStore.getApiUser().name //TODO display name of user (most likely this will only happen in "sent")
        listItemView.findViewById<TextView>(R.id.mail_date).text =
            TextUtils.parseShortDate(item.getDate())

        listItemView.findViewById<ImageView>(R.id.mail_answered).visibility =
            if (item.isAnswered()) View.VISIBLE else View.INVISIBLE
        listItemView.findViewById<ImageView>(R.id.mail_flagged).visibility =
            if (item.isFlagged()) View.VISIBLE else View.INVISIBLE

        return listItemView
    }

    override fun search(constraint: String?): List<IEmail> {
        if (constraint == null)
            return originalElements
        return originalElements.filter { mail ->
            mail.getSubject().filterApplies(constraint) ||
                    mail.getFrom()?.any { it.filterApplies(constraint) } == true
        }
    }

    override fun sort(elements: List<IEmail>): List<IEmail> {
        return elements.sortedByDescending { it.getDate() }
    }
}