package de.deftk.openww.android.fragments.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import de.deftk.openww.android.R

class PrivacyDialogFragment : DialogFragment() {

    companion object {
        const val PRIVACY_STATEMENT_SHOWN_KEY = "privacy_statement_shown"
    }

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }
    private val navController by lazy { findNavController() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.privacy_title)
        val textView = TextView(requireContext())
        textView.setText(R.string.privacy_short)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(50, 50, 50, 50)
        textView.layoutParams = params
        val layout = LinearLayout(requireContext())
        layout.addView(textView, params)
        builder.setView(layout)
        builder.setPositiveButton(R.string.confirm) { _, _ ->
            preferences.edit().putBoolean(PRIVACY_STATEMENT_SHOWN_KEY, true).apply()
            navController.navigate(PrivacyDialogFragmentDirections.actionPrivacyDialogFragmentToLaunchFragment())
        }
        builder.setNegativeButton(R.string.cancel) { _, _ ->
            requireActivity().finish()
        }
        return builder.create()
    }

}