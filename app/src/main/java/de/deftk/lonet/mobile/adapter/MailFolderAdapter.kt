package de.deftk.lonet.mobile.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import de.deftk.lonet.api.model.feature.mailbox.EmailFolder
import de.deftk.lonet.mobile.R

class MailFolderAdapter(context: Context, elements: List<EmailFolder>) :
    ArrayAdapter<EmailFolder>(context, 0, elements) {

    companion object {
        private val defaultFolderTranslations = mapOf(
            Pair(EmailFolder.EmailFolderType.INBOX, R.string.mail_folder_inbox),
            Pair(EmailFolder.EmailFolderType.DRAFTS, R.string.mail_folder_drafts),
            Pair(EmailFolder.EmailFolderType.SENT, R.string.mail_folder_sent),
            Pair(EmailFolder.EmailFolderType.TRASH, R.string.mail_folder_trash)
        )
        private val folderImages = mapOf(
            Pair(EmailFolder.EmailFolderType.INBOX, R.drawable.ic_inbox_24),
            Pair(EmailFolder.EmailFolderType.DRAFTS, R.drawable.ic_drafts_24),
            Pair(EmailFolder.EmailFolderType.SENT, R.drawable.ic_send_24),
            Pair(EmailFolder.EmailFolderType.TRASH, R.drawable.ic_delete_24)
        )

        fun getDefaultFolderTranslation(context: Context, folder: EmailFolder): String {
            return if (defaultFolderTranslations.containsKey(folder.type))
                context.getString(defaultFolderTranslations.getValue(folder.type))
            else folder.name
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_mail_folder, parent, false)
        val item = getItem(position) ?: return listItemView
        listItemView.findViewById<TextView>(R.id.mail_folder_name).text = getDefaultFolderTranslation(context, item)
        listItemView.findViewById<ImageView>(R.id.mail_folder_image)
            .setImageResource(folderImages[item.type] ?: R.drawable.ic_folder_special_24)
        return listItemView
    }

}