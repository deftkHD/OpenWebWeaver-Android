package de.deftk.lonet.mobile.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.daimajia.swipe.SwipeLayout
import de.deftk.lonet.api.model.feature.mailbox.Email
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.utils.SwipeAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat

class MailAdapter(context: Context, elements: List<Email>): ArrayAdapter<Email>(context, 0, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = (convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_mail, parent, false)) as SwipeLayout
        val item = getItem(position) ?: return listItemView

        listItemView.addSwipeListener(object : SwipeAdapter() {
            override fun onOpen(layout: SwipeLayout) {
                CoroutineScope(Dispatchers.IO).launch {
                    item.delete()
                    withContext(Dispatchers.Main) {
                        remove(item)
                        notifyDataSetChanged()
                    }
                }
            }
        })

        val subjectView = listItemView.findViewById<TextView>(R.id.mail_subject)
        subjectView.text = item.subject
        if (item.isRead != false)
            subjectView.setTypeface(null, Typeface.NORMAL)
        else
            subjectView.setTypeface(null, Typeface.BOLD)
        listItemView.findViewById<TextView>(R.id.mail_author).text = item.from?.joinToString { it.name } ?: AuthStore.appUser.getName() //TODO not verified if this is useful
        listItemView.findViewById<TextView>(R.id.mail_date).text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(item.date)

        return listItemView
    }

}