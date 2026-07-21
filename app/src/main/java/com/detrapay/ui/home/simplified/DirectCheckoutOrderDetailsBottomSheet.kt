package com.detrapay.ui.home.simplified

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import com.detrapay.R
import com.detrapay.databinding.BottomSheetDirectCheckoutOrderDetailsBinding
import com.detrapay.ui.order_details.OrderDetailsPaymentMethodPickerBottomSheet
import com.detrapay.ui.util.PaymentTypeRules
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class DirectCheckoutOrderDetailsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDirectCheckoutOrderDetailsBinding? = null
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
        _binding = BottomSheetDirectCheckoutOrderDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val orderId = requireArguments().getInt(ARG_ORDER_ID)
        val customerName = requireArguments().getString(ARG_CUSTOMER_NAME).orEmpty()
        val pendingAmount = requireArguments().getString(ARG_PENDING_AMOUNT).orEmpty()
        val availableTypes = requireArguments().getStringArrayList(ARG_AVAILABLE_TYPES).orEmpty()

        binding.orderDetailsTitle.text = getString(R.string.direct_checkout_receive_order_title, orderId)
        binding.orderCustomerName.text = customerName.ifBlank { getString(R.string.registration_payment_customer_fallback) }
        binding.orderPendingAmount.text = pendingAmount
        binding.btnCancel.setOnClickListener { dismiss() }

        binding.paymentMethodsContainer.removeAllViews()
        availableTypes.forEach { type ->
            binding.paymentMethodsContainer.addView(createPaymentButton(type))
        }
    }

    private fun createPaymentButton(type: String): MaterialButton {
        return MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.ds_touch_target),
            ).also {
                it.topMargin = resources.getDimensionPixelSize(R.dimen.spacing_small)
            }
            text = paymentTypeLabel(type)
            textSize = 16f
            isAllCaps = false
            setOnClickListener {
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(RESULT_PAYMENT_TYPE to type),
                )
                dismiss()
            }
        }
    }

    private fun paymentTypeLabel(type: String): String {
        return when (PaymentTypeRules.normalize(type)) {
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT -> "Credito"
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_DEBIT -> "Debito"
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_PIX -> "Pix"
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CASH -> "Dinheiro"
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_STORE_CREDIT -> "Credito loja"
            else -> type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    companion object {
        const val REQUEST_KEY = "direct_checkout_order_details_request"
        const val RESULT_PAYMENT_TYPE = "payment_type"

        private const val ARG_ORDER_ID = "orderId"
        private const val ARG_CUSTOMER_NAME = "customerName"
        private const val ARG_PENDING_AMOUNT = "pendingAmount"
        private const val ARG_AVAILABLE_TYPES = "availableTypes"

        fun newInstance(
            orderId: Int,
            customerName: String,
            pendingAmount: String,
            availableTypes: List<String>,
        ): DirectCheckoutOrderDetailsBottomSheet {
            return DirectCheckoutOrderDetailsBottomSheet().apply {
                arguments = bundleOf(
                    ARG_ORDER_ID to orderId,
                    ARG_CUSTOMER_NAME to customerName,
                    ARG_PENDING_AMOUNT to pendingAmount,
                    ARG_AVAILABLE_TYPES to ArrayList(availableTypes),
                )
            }
        }
    }
}
