package com.detrapay.ui.registration.payment_method

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.databinding.BottomSheetInstallmentsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class InstallmentsBottomSheet(
    private val installments: List<InstallmentFee>,
    private val onSelected: (InstallmentFee) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetInstallmentsBinding

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetInstallmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val adapter = InstallmentsAdapter(showRadioButton = true) { fee ->
            onSelected(fee)
            dismiss()
        }
        binding.rvInstallmentsBottomSheet.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInstallmentsBottomSheet.adapter = adapter
        adapter.submitList(installments)
    }

    companion object {
        const val TAG = "InstallmentsBottomSheet"
    }
}
