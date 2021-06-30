package de.deftk.openww.android.fragments.feature.messenger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ChatMessageAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentMessengerChatBinding
import de.deftk.openww.android.feature.filestorage.DownloadOpenWorker
import de.deftk.openww.android.utils.FileUtil
import de.deftk.openww.android.viewmodel.MessengerViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.feature.FileUrl
import java.io.File


class MessengerChatFragment : Fragment(), AttachmentDownloader {

    private val userViewModel: UserViewModel by activityViewModels()
    private val messengerViewModel: MessengerViewModel by activityViewModels()
    private val args: MessengerChatFragmentArgs by navArgs()
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    private lateinit var binding: FragmentMessengerChatBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMessengerChatBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()

        val adapter = ChatMessageAdapter(userViewModel, this)
        binding.chatList.adapter = adapter

        binding.chatsSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                messengerViewModel.loadHistory(false, apiContext)
            }
        }

        binding.btnSend.setOnClickListener {
            userViewModel.apiContext.value?.also { apiContext ->
                messengerViewModel.sendMessage(args.user, binding.txtMessage.text.toString(), null, apiContext)
            }
        }

        messengerViewModel.sendMessageResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                binding.txtMessage.text = null
                userViewModel.apiContext.value?.also { apiContext ->
                    messengerViewModel.loadHistory(true, apiContext)
                }
            } else if (response is Response.Failure) {
                //TODO handle error
                response.exception.printStackTrace()
            }
        }

        messengerViewModel.messagesResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                val messages = response.value.first.filter { it.to.login == args.user || it.from.login == args.user }
                if ((adapter.itemCount != messages.size && response.value.second) || !response.value.second)
                    adapter.submitList(messages)
                binding.chatsEmpty.isVisible = messages.isEmpty()
            } else if (response is Response.Failure) {
                //TODO handle error
                response.exception.printStackTrace()
            }
            binding.progressChats.isVisible = false
            binding.chatsSwipeRefresh.isRefreshing = false
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                messengerViewModel.loadHistory(false, apiContext)
            } else {
                findNavController().popBackStack(R.id.chatsFragment, false)
            }
        }

        return binding.root
    }

    override fun downloadAttachment(url: FileUrl, name: String) {
        val workManager = WorkManager.getInstance(requireContext())
        val tempDir = File(requireActivity().cacheDir, "attachments")
        if (!tempDir.exists())
            tempDir.mkdir()
        val tempFile = File(tempDir, FileUtil.escapeFileName(url.name ?: name))
        val workRequest = DownloadOpenWorker.createRequest(tempFile.absolutePath, url.url, url.name ?: name, url.size?.toLong() ?: error("No attachment size"))
        workManager.enqueue(workRequest)
        workManager.getWorkInfoByIdLiveData(workRequest.id).observe(viewLifecycleOwner) { workInfo ->
            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                val fileUri = Uri.parse(workInfo.outputData.getString(DownloadOpenWorker.DATA_FILE_URI))
                val fileName = workInfo.outputData.getString(DownloadOpenWorker.DATA_FILE_NAME)!!

                val mime = FileUtil.getMimeType(fileName)
                val sendIntent = Intent(Intent.ACTION_SEND)
                sendIntent.type = mime
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, FileUtil.normalizeFileName(fileName, preferences))
                val viewIntent = Intent(Intent.ACTION_VIEW)
                viewIntent.setDataAndType(fileUri, mime)
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(sendIntent, fileName).apply { putExtra(
                    Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent)) })
            } else if (workInfo.state == WorkInfo.State.FAILED) {
                //TODO handle error
            }
        }
    }
}

interface AttachmentDownloader {
    fun downloadAttachment(url: FileUrl, name: String)
}