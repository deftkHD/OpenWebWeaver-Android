package de.deftk.openww.android.fragments.devtools

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.FragmentPastRequestBinding
import de.deftk.openww.android.feature.devtools.PastRequest
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.LoginViewModel
import de.deftk.openww.api.WebWeaverClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.DateFormat

class PastRequestFragment : AbstractFragment(true) {

    private val args: PastRequestFragmentArgs by navArgs()
    private val loginViewModel by activityViewModels<LoginViewModel>()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentPastRequestBinding
    private lateinit var response: PastRequest

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPastRequestBinding.inflate(inflater, container, false)

        loginViewModel.pastRequests.observe(viewLifecycleOwner) { responses ->
            val resp = responses.singleOrNull { it.id == args.requestId }
            if (resp == null) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_past_request_not_found, args.requestId.toString(), requireContext())
                navController.popBackStack()
                return@observe
            }
            response = resp


            binding.requestDate.text = getString(R.string.response_at).format(DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.LONG).format(response.responseDate))
            binding.requestTitle.text = response.getTitle()
            @SuppressLint("SetTextI18n") // it's ok, no need to worry
            binding.requestText.text = "[\n${response.request.requests.joinToString(",\n") { it.toString() }}\n]"
            binding.responseText.text = response.response.text

            setUIState(UIState.READY)
        }

        binding.fabCopyRequestData.setOnClickListener {
            val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipObj = buildJsonObject {
                put("requestId", response.id)
                put("date", response.responseDate.time)
                put("request", "[\n${response.request.requests.joinToString(",\n") { it.toString() }}\n]")
                put("response", response.response.text)
            }
            clipboard.setPrimaryClip(ClipData.newPlainText("Request data", WebWeaverClient.json.encodeToString(clipObj)))
        }

        return binding.root
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.fabCopyRequestData.isEnabled = newState == UIState.READY
    }
}