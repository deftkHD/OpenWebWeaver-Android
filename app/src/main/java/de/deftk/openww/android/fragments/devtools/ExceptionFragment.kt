package de.deftk.openww.android.fragments.devtools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.FragmentExceptionBinding
import de.deftk.openww.android.feature.devtools.ExceptionReport
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.DebugUtil
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.api.WebWeaverClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.DateFormat

class ExceptionFragment : AbstractFragment(true) {

    private val args: ExceptionFragmentArgs by navArgs()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentExceptionBinding
    private lateinit var report: ExceptionReport

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentExceptionBinding.inflate(inflater, container, false)

        DebugUtil.exceptions.observe(viewLifecycleOwner) { responses ->
            val rep = responses.singleOrNull { it.id == args.exceptionId }
            if (rep == null) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_exception_not_found, args.exceptionId.toString(), requireContext())
                navController.popBackStack()
                return@observe
            }
            report = rep

            binding.exceptionDate.text = getString(R.string.exception_at).format(DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.LONG).format(report.date))
            binding.exceptionName.text = report.getTitle()
            binding.exceptionDetail.text = report.exception?.stackTraceToString() ?: report.stackTrace.joinToString("\n") { "\tat $it" }
            binding.exceptionSource.text = getString(R.string.source).format(report.source.toString())

            binding.fabCopyExceptionData.isEnabled = true

            setUIState(UIState.READY)
        }

        binding.fabCopyExceptionData.setOnClickListener {
            val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipObj = buildJsonObject {
                put("exceptionId", report.id)
                put("name", report.getTitle())
                put("date", report.date.time)
                put("source", report.source.toString())
                put("stacktrace", report.exception?.stackTraceToString() ?: report.stackTrace.joinToString("\n") { "\tat $it" })
            }
            clipboard.setPrimaryClip(ClipData.newPlainText("Exception", WebWeaverClient.json.encodeToString(clipObj)))
        }

        return binding.root
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.fabCopyExceptionData.isEnabled = newState == UIState.READY
    }
}