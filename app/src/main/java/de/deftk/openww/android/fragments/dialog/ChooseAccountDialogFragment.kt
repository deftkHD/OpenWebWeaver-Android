package de.deftk.openww.android.fragments.dialog

import android.accounts.AccountManager
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.AccountAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.auth.AuthHelper
import de.deftk.openww.android.auth.WebWeaverAuthenticator
import de.deftk.openww.android.viewmodel.UserViewModel

class ChooseAccountDialogFragment : DialogFragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private var actionPerformed = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        userViewModel.loginResponse.observe(this) { response ->
            if (actionPerformed) {
                if (response is Response.Success) {
                    AuthHelper.rememberLogin(response.value.getUser().login, requireContext())
                    navController.navigate(ChooseAccountDialogFragmentDirections.actionChooseAccountDialogFragmentToOverviewFragment())
                } else if (response is Response.Failure) {
                    response.exception.printStackTrace()
                    actionPerformed = false
                    //TODO handle error
                }
            }
        }

        val accountManager = AccountManager.get(requireContext())
        val accounts = accountManager.getAccountsByType(getString(R.string.account_type))
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.choose_account)
        builder.setIcon(R.drawable.ic_account_circle_24)
        val accountAdapter = AccountAdapter(requireContext(), accounts.toList())
        builder.setAdapter(accountAdapter) { _, which ->
            actionPerformed = true
            val account = accounts[which]
            userViewModel.loginAccount(account, accountManager.getPassword(account))
        }
        return builder.create()
    }

}