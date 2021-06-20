package de.deftk.openlonet.adapter

import android.accounts.Account
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import de.deftk.openlonet.R

class AccountAdapter(context: Context, elements: List<Account>): ArrayAdapter<Account>(context, 0, elements) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_account, parent, false)
        val item = getItem(position) ?: return listItemView
        val split = item.name.split("@")
        listItemView.findViewById<TextView>(R.id.account_name).text = split[0]
        listItemView.findViewById<TextView>(R.id.account_provider).text = split[1]
        return listItemView
    }

}