package de.deftk.openlonet.fragments.dialog

import android.accounts.Account
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import de.deftk.openlonet.R

class ChooseAccountDialogFragment(private val accounts: Array<Account>, private val listener: AccountSelectListener) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { it ->
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.choose_account)
            builder.setItems(accounts.map { account -> account.name }.toTypedArray()) { _, which ->
                listener.onAccountSelected(accounts[which])
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    interface AccountSelectListener {
        fun onAccountSelected(account: Account)
    }

}