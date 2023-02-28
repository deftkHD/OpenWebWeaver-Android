package de.deftk.openww.android.fragments.devtools.permission

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.PermissionAdapter
import de.deftk.openww.android.databinding.FragmentPermissionsBinding
import de.deftk.openww.android.fragments.AbstractFragment
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.IGroup

class PermissionsFragment : AbstractFragment(true) {

    private val args: PermissionsFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentPermissionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPermissionsBinding.inflate(inflater, container, false)

        val effectivePermissionAdapter = PermissionAdapter()
        binding.effectivePermissionList.adapter = effectivePermissionAdapter
        binding.effectivePermissionList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        val basePermissionAdapter = PermissionAdapter()
        binding.basePermissionList.adapter = basePermissionAdapter
        binding.effectivePermissionList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                val scope = apiContext.findOperatingScope(args.scope)
                if (scope == null) {
                    Reporter.reportException(R.string.error_scope_not_found, args.scope, requireContext())
                    navController.popBackStack()
                    return@observe
                }

                val basePermissionText = resources.getQuantityText(R.plurals.base_permissions_num, scope.baseRights.size).toString().format(scope.baseRights.size)
                binding.btnBasePermissions.text = basePermissionText
                binding.btnBasePermissions.textOff = basePermissionText
                binding.btnBasePermissions.textOn = basePermissionText
                binding.btnBasePermissions.setOnClickListener {
                    binding.basePermissionList.isVisible = !binding.basePermissionList.isVisible
                }
                basePermissionAdapter.submitList(scope.baseRights)

                val effectivePermissionText = resources.getQuantityText(R.plurals.effective_permissions_num, scope.effectiveRights.size).toString().format(scope.effectiveRights.size)
                binding.btnEffectivePermissions.text = effectivePermissionText
                binding.btnEffectivePermissions.textOff = effectivePermissionText
                binding.btnEffectivePermissions.textOn = effectivePermissionText
                binding.btnEffectivePermissions.setOnClickListener {
                    binding.effectivePermissionList.isVisible = !binding.effectivePermissionList.isVisible
                }
                effectivePermissionAdapter.submitList(scope.effectiveRights)

                if (scope is IGroup) {
                    val memberPermissionAdapter = PermissionAdapter()
                    binding.memberPermissionList.adapter = memberPermissionAdapter
                    binding.memberPermissionList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

                    val reducedPermissionAdapter = PermissionAdapter()
                    binding.reducedPermissionList.adapter = reducedPermissionAdapter
                    binding.reducedPermissionList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))


                    val memberPermissionText = resources.getQuantityText(R.plurals.member_permissions_num, scope.memberRights.size).toString().format(scope.memberRights.size)
                    binding.btnMemberPermissions.text = memberPermissionText
                    binding.btnMemberPermissions.textOff = memberPermissionText
                    binding.btnMemberPermissions.textOn = memberPermissionText
                    binding.btnMemberPermissions.setOnClickListener {
                        binding.memberPermissionList.isVisible = !binding.memberPermissionList.isVisible
                    }
                    memberPermissionAdapter.submitList(scope.memberRights)

                    val reducedPermissionText = resources.getQuantityText(R.plurals.reduced_permissions_num, scope.reducedRights.size).toString().format(scope.reducedRights.size)
                    binding.btnReducedPermissions.text = reducedPermissionText
                    binding.btnReducedPermissions.textOff = reducedPermissionText
                    binding.btnReducedPermissions.textOn = reducedPermissionText
                    binding.btnReducedPermissions.setOnClickListener {
                        binding.reducedPermissionList.isVisible = !binding.reducedPermissionList.isVisible
                    }
                    reducedPermissionAdapter.submitList(scope.reducedRights)
                } else {
                    binding.btnMemberPermissions.isVisible = false
                    binding.memberPermissionList.isVisible = false
                    binding.btnReducedPermissions.isVisible = false
                    binding.reducedPermissionList.isVisible = false
                }

                setUIState(UIState.READY)
            } else {
                effectivePermissionAdapter.submitList(emptyList())
                setUIState(UIState.DISABLED)
            }
        }

        return binding.root
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.effectivePermissionList.isEnabled = newState.listEnabled
        binding.basePermissionList.isEnabled = newState.listEnabled
        binding.memberPermissionList.isEnabled = newState.listEnabled
        binding.reducedPermissionList.isEnabled = newState.listEnabled
        binding.btnBasePermissions.isEnabled = newState == UIState.READY
        binding.btnEffectivePermissions.isEnabled = newState == UIState.READY
        binding.btnMemberPermissions.isEnabled = newState == UIState.READY
        binding.btnReducedPermissions.isEnabled = newState == UIState.READY
        binding.permissionsEmpty.isVisible = newState.showEmptyIndicator
    }

}