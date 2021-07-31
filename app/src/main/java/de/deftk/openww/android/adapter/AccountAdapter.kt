package de.deftk.openww.android.adapter

import android.accounts.Account
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import de.deftk.openww.android.R
import de.deftk.openww.android.viewmodel.UserViewModel

class AccountAdapter(context: Context, elements: List<Account>, private val userViewModel: UserViewModel): ArrayAdapter<Account>(context, 0, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_account, parent, false)
        val item = getItem(position) ?: return listItemView
        val split = item.name.split("@")
        val titleTextView = listItemView.findViewById<TextView>(R.id.account_name)
        titleTextView.text = split[0]
        if (item.name == userViewModel.apiContext.value?.user?.login) {
            titleTextView.setTypeface(null, Typeface.BOLD)
            listItemView.setBackgroundResource(R.drawable.selected_account_item)
        } else {
            titleTextView.setTypeface(null, Typeface.NORMAL)
            listItemView.setBackgroundResource(android.R.color.transparent)
        }
        listItemView.findViewById<TextView>(R.id.account_provider).text = split[1]
        return listItemView
    }

}