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
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.LoginViewModel

class ChooseAccountDialogFragment : DialogFragment() {

    private val loginViewModel by activityViewModels<LoginViewModel>()
    private val navController by lazy { findNavController() }

    private var actionPerformed = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        loginViewModel.loginResponse.observe(this) { response ->
            if (actionPerformed) {
                if (response is Response.Success) {
                    navController.navigate(ChooseAccountDialogFragmentDirections.actionChooseAccountDialogFragmentToOverviewFragment())
                } else if (response is Response.Failure) {
                    Reporter.reportException(R.string.error_login_failed, response.exception, requireContext())
                    actionPerformed = false
                    navController.popBackStack()
                }
            }
        }

        val accountManager = AccountManager.get(requireContext())
        val accounts = accountManager.getAccountsByType(getString(R.string.account_type))
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.choose_account)
        builder.setIcon(R.drawable.ic_account_circle_24)
        val accountAdapter = AccountAdapter(requireContext(), accounts.toList(), loginViewModel)
        builder.setAdapter(accountAdapter) { _, which ->
            actionPerformed = true
            val account = accounts[which]
            loginViewModel.loginAccount(account, accountManager.getPassword(account))
        }
        return builder.create()
    }

}