package de.deftk.openww.android.fragments.feature.members

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.MemberAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentMembersBinding
import de.deftk.openww.android.viewmodel.GroupViewModel
import de.deftk.openww.android.viewmodel.UserViewModel

class MembersFragment : Fragment() {

    private val args: MembersFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val groupViewModel: GroupViewModel by activityViewModels()

    private lateinit var binding: FragmentMembersBinding
    private lateinit var group: IGroup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMembersBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        group = userViewModel.apiContext.value?.getUser()?.getGroups()?.firstOrNull { it.login == args.groupId } ?: error("Failed to find given group")

        val adapter = MemberAdapter()
        binding.memberList.adapter = adapter
        binding.memberList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        groupViewModel.getGroupMembers(group).observe(viewLifecycleOwner) { resource ->
            if (resource is Response.Success) {
                adapter.submitList(resource.value)
                binding.membersEmpty.isVisible = resource.value.isEmpty()
            } else if (resource is Response.Failure) {
                //TODO handle error
                resource.exception.printStackTrace()
            }
            binding.progressMembers.isVisible = false
            binding.membersSwipeRefresh.isRefreshing = false
        }

        binding.membersSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                groupViewModel.loadMembers(group, false, apiContext)
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                groupViewModel.loadMembers(group, false, apiContext)
            } else {
                findNavController().popBackStack(R.id.membersGroupFragment, false)
            }
        }

        setHasOptionsMenu(true)
        registerForContextMenu(binding.memberList)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        requireActivity().menuInflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                //TODO search
                return false
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            val member = (binding.memberList.adapter as MemberAdapter).getItem(menuInfo.position)
            if (member.login != userViewModel.apiContext.value?.getUser()?.login) {
                requireActivity().menuInflater.inflate(R.menu.member_action_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.memberList.adapter as MemberAdapter
        return when (item.itemId) {
            R.id.member_action_write_mail -> {
                val member = adapter.getItem(menuInfo.position)
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(member.login)}"))
                startActivity(Intent.createChooser(intent, getString(R.string.send_mail)))
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

}