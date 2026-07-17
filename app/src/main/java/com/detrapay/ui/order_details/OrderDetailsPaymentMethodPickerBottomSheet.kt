package com.detrapay.ui.order_details

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.detrapay.databinding.BottomSheetPaymentMethodPickerBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class OrderDetailsPaymentMethodPickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPaymentMethodPickerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetPaymentMethodPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val availableTypes = (arguments?.getStringArrayList(ARG_AVAILABLE_TYPES) ?: arrayListOf())
            .toSet()

        binding.optionCredit.visibility = if (availableTypes.contains(TYPE_CREDIT)) View.VISIBLE else View.GONE
        binding.optionDebit.visibility = if (availableTypes.contains(TYPE_DEBIT)) View.VISIBLE else View.GONE
        binding.optionPix.visibility = if (availableTypes.contains(TYPE_PIX)) View.VISIBLE else View.GONE
        binding.optionCash.visibility = if (availableTypes.contains(TYPE_CASH)) View.VISIBLE else View.GONE
        binding.optionStoreCredit.visibility = if (availableTypes.contains(TYPE_STORE_CREDIT)) View.VISIBLE else View.GONE

        binding.optionCredit.setOnClickListener { select(TYPE_CREDIT) }
        binding.optionDebit.setOnClickListener { select(TYPE_DEBIT) }
        binding.optionPix.setOnClickListener { select(TYPE_PIX) }
        binding.optionCash.setOnClickListener { select(TYPE_CASH) }
        binding.optionStoreCredit.setOnClickListener { select(TYPE_STORE_CREDIT) }
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun select(type: String) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            bundleOf(RESULT_PAYMENT_TYPE to type),
        )
        dismiss()
    }

    companion object {
        const val REQUEST_KEY = "order_details_payment_method_picker_request"
        const val RESULT_PAYMENT_TYPE = "payment_type"

        const val TYPE_CREDIT = "credito"
        const val TYPE_DEBIT = "debito"
        const val TYPE_PIX = "pix"
        const val TYPE_CASH = "dinheiro"
        const val TYPE_STORE_CREDIT = "store_credit"

        private const val ARG_AVAILABLE_TYPES = "available_types"

        fun newInstance(availableTypes: List<String>): OrderDetailsPaymentMethodPickerBottomSheet {
            return OrderDetailsPaymentMethodPickerBottomSheet().apply {
                arguments = bundleOf(ARG_AVAILABLE_TYPES to ArrayList(availableTypes))
            }
        }
    }
}
