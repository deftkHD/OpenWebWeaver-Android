package de.deftk.openww.android.fragments.feature.messenger

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.core.view.isVisible
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
import de.deftk.openww.android.feature.AbstractNotifyingWorker
import de.deftk.openww.android.feature.filestorage.DownloadOpenWorker
import de.deftk.openww.android.filter.MessageFilter
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.FileUtil
import de.deftk.openww.android.utils.ISearchProvider
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.MessengerViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.feature.FileUrl
import java.io.File


class MessengerChatFragment : AbstractFragment(true), AttachmentDownloader, ISearchProvider {

    private val userViewModel: UserViewModel by activityViewModels()
    private val messengerViewModel: MessengerViewModel by activityViewModels()
    private val args: MessengerChatFragmentArgs by navArgs()
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    private lateinit var binding: FragmentMessengerChatBinding
    private lateinit var searchView: SearchView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMessengerChatBinding.inflate(inflater, container, false)

        val adapter = ChatMessageAdapter(userViewModel, this, findNavController(), userViewModel.apiContext.value?.user!!)
        binding.chatList.adapter = adapter

        binding.chatsSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                messengerViewModel.loadHistory(args.user, false, apiContext)
            }
        }

        binding.btnSend.setOnClickListener {
            userViewModel.apiContext.value?.also { apiContext ->
                messengerViewModel.sendMessage(args.user, binding.txtMessage.text.toString(), null, apiContext)
                enableUI(false)
            }
        }

        messengerViewModel.sendMessageResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                binding.txtMessage.text = null
                userViewModel.apiContext.value?.also { apiContext ->
                    messengerViewModel.loadHistory(args.user, true, apiContext)
                }
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_send_message_failed, response.exception, requireContext())
            }
        }

        messengerViewModel.getFilteredMessagesResponse(args.user).observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                val messages = response.value.first
                if ((adapter.itemCount != messages.size && response.value.second) || !response.value.second)
                    adapter.submitList(messages)
                binding.chatsEmpty.isVisible = messages.isEmpty()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_messages_failed, response.exception, requireContext())
            }
            enableUI(true)
            binding.chatsSwipeRefresh.isRefreshing = false
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                messengerViewModel.loadHistory(args.user, false, apiContext)
                if (messengerViewModel.getAllMessagesResponse(args.user).value == null)
                    enableUI(false)
            } else {
                //TODO implement
                findNavController().popBackStack(R.id.chatsFragment, false)
            }
        }

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun downloadAttachment(url: FileUrl, name: String) {
        val workManager = WorkManager.getInstance(requireContext())
        val tempDir = File(requireActivity().cacheDir, "attachments")
        if (!tempDir.exists())
            tempDir.mkdir()
        val tempFile = File(tempDir, FileUtil.escapeFileName(url.name ?: name))
        if (url.size == null) {
            Reporter.reportException(R.string.error_invalid_size, "null", requireContext())
            return
        }
        val workRequest = DownloadOpenWorker.createRequest(tempFile.absolutePath, url.url, url.name ?: name, url.size!!)
        workManager.enqueue(workRequest)
        workManager.getWorkInfoByIdLiveData(workRequest.id).observe(viewLifecycleOwner) { workInfo ->
            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                val fileUri = Uri.parse(workInfo.outputData.getString(DownloadOpenWorker.DATA_FILE_URI))
                val fileName = workInfo.outputData.getString(DownloadOpenWorker.DATA_FILE_NAME)!!
                FileUtil.showFileOpenIntent(fileName, fileUri, preferences, requireContext())
            } else if (workInfo.state == WorkInfo.State.FAILED) {
                val errorMessage = workInfo.outputData.getString(AbstractNotifyingWorker.DATA_ERROR_MESSAGE) ?: "Unknown"
                Reporter.reportException(R.string.error_download_worker_failed, errorMessage, requireContext())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.messenger_chat_options_menu, menu)
        inflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        searchView = searchItem.actionView as SearchView
        searchView.setQuery(messengerViewModel.messageFilter.value?.smartSearchCriteria?.value, false) // restore recent search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filter = MessageFilter()
                filter.smartSearchCriteria.value = newText
                messengerViewModel.messageFilter.value = filter
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onSearchBackPressed(): Boolean {
        return if (searchView.isIconified) {
            false
        } else {
            searchView.isIconified = true
            searchView.setQuery(null, true)
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_saved_chat -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    messengerViewModel.clearChat(args.user, apiContext)
                    enableUI(false)
                }
            }
            else -> return false
        }
        return true
    }

    override fun onUIStateChanged(enabled: Boolean) {
        binding.btnSend.isEnabled = enabled
        binding.txtMessage.isEnabled = enabled
        binding.chatsSwipeRefresh.isEnabled = enabled
        binding.chatList.isEnabled = enabled
    }
}

interface AttachmentDownloader {
    fun downloadAttachment(url: FileUrl, name: String)
}