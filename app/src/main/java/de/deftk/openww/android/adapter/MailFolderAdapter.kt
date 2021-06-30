package de.deftk.openww.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import de.deftk.openww.android.R
import de.deftk.openww.api.model.feature.mailbox.IEmailFolder

class MailFolderAdapter(context: Context, val elements: List<IEmailFolder>) : ArrayAdapter<IEmailFolder>(context, 0, elements) {

    companion object {
        fun getTranslatedFolderName(context: Context, folder: IEmailFolder): String {
            val resource = when {
                folder.isInbox -> R.string.mail_folder_inbox
                folder.isDrafts -> R.string.mail_folder_drafts
                folder.isSent -> R.string.mail_folder_sent
                folder.isTrash -> R.string.mail_folder_trash
                else -> null
            }
            return if (resource != null) context.getString(resource) else folder.getName()
        }

        fun getFolderIcon(folder: IEmailFolder): Int {
            return when {
                folder.isInbox -> R.drawable.ic_inbox_24
                folder.isDrafts -> R.drawable.ic_drafts_24
                folder.isSent -> R.drawable.ic_send_24
                folder.isTrash -> R.drawable.ic_delete_24
                else -> R.drawable.ic_folder_special_24
            }
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_mail_folder, parent, false)
        val item = getItem(position) ?: return listItemView
        listItemView.findViewById<TextView>(R.id.mail_folder_name).text = getTranslatedFolderName(context, item)
        listItemView.findViewById<ImageView>(R.id.mail_folder_image).setImageResource(getFolderIcon(item))
        return listItemView
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }

}