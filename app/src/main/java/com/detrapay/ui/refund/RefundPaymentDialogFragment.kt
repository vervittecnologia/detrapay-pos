package com.detrapay.ui.refund

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.detrapay.R
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.RefundPaymentData
import com.detrapay.databinding.RefundPaymentDialogBinding
import com.detrapay.ui.state.UIState

class RefundPaymentDialogFragment(
    private val listener: RefundPaymentListener,
    private val receivableItem: OrderReceivableItem
) : DialogFragment() {

    private lateinit var binding: RefundPaymentDialogBinding
    private val viewModel: RefundPaymentDialogViewModel by activityViewModels()

    private var result: RefundPaymentData? = null

    interface RefundPaymentListener {
        fun onResult(refundPaymentData: RefundPaymentData?)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = RefundPaymentDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupView()
        startRefund(receivableItem)
    }

    private fun startRefund(receivable: OrderReceivableItem){
        viewModel.refundPayment(receivable)
    }

    private fun setupObservers() {
        viewModel.init()
        viewModel.paymentState.observe(this, Observer { status ->
            when (status) {
                is UIState.Loading -> {
                    status.message.let {
                        binding.refundMessage.text = it
                    }
                }

                is UIState.Success -> {
                    status.data?.let {
                        result = it
                        binding.loadingView.visibility = View.GONE
                    }
                }

                is UIState.Error -> {
                    binding.loadingView.visibility = View.GONE
                    binding.refundMessage.text = status.message ?: getString(R.string.employees_default_error_message)
                }
            }
        })
    }

    private fun setupView() {
        binding.closeDialog.setOnClickListener {
            viewModel.abortPayment()
            this.dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        listener.onResult(result)
        super.onDismiss(dialog)
    }
}