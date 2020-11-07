package de.deftk.lonet.mobile.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import de.deftk.lonet.api.model.feature.mailbox.Email
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import java.text.DateFormat

class MailAdapter(context: Context, elements: List<Email>): ArrayAdapter<Email>(context, 0, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_mail, parent, false)
        val item = getItem(position) ?: return listItemView

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