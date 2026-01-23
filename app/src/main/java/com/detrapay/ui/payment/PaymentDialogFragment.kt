package com.detrapay.ui.payment

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
import com.detrapay.data.model.PaymentData
import com.detrapay.databinding.PaymentDialogBinding
import com.detrapay.ui.state.UIState
import java.util.Locale

class PaymentDialogFragment(
    private val listener: PaymentListener,
    private val orderId: Int,
    private val receivableItem: OrderReceivableItem
) : DialogFragment() {

    private val locale = Locale("pt", "BR")
    private lateinit var binding: PaymentDialogBinding
    private val viewModel: PaymentDialogViewModel by activityViewModels()

    private var result: PaymentData? = null

    interface PaymentListener {
        fun onResult(paymentData: PaymentData?)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PaymentDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupView(orderId, receivableItem)
        startPayment(orderId, receivableItem)
    }

    private fun startPayment(orderId: Int, receivable: OrderReceivableItem){
        viewModel.payOrder(orderId, receivable)
    }

    private fun setupObservers() {
        viewModel.init()
        viewModel.paymentState.observe(this, Observer { status ->
            when (status) {
                is UIState.Loading -> {
                    status.message.let {
                        binding.transactionMessage.text = it
                    }
                    binding.successView.visibility = View.GONE
                    binding.errorView.visibility = View.GONE
                    binding.loadingView.visibility = View.VISIBLE
                }

                is UIState.Success -> {
                    status.data?.let {
                        result = it
                        binding.errorView.visibility = View.GONE
                        binding.loadingView.visibility = View.GONE
                        binding.successView.visibility = View.VISIBLE
                    }
                }

                is UIState.Error -> {
                    binding.loadingView.visibility = View.GONE
                    binding.successView.visibility = View.GONE
                    binding.errorMessage.text = status.message ?: getString(R.string.employees_default_error_message)
                    binding.errorView.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun setupView(orderId: Int, receivableItem: OrderReceivableItem) {
        val paymentMethodName = receivableItem.paymentMethod.name

        val isCreditCard = receivableItem.paymentMethod.name.contains("Crédito", true) ||
                receivableItem.paymentMethod.name.contains("Débito", true) ||
                receivableItem.paymentMethod.name.contains("Cartão de crédito", true) ||
                receivableItem.paymentMethod.name.contains("VISA", true) ||
                receivableItem.paymentMethod.name.contains("Mastercard", true)

        val receivableAmount = if (isCreditCard) {
            val amount = "%,.2f".format(locale, receivableItem.amountFinal)
            val installmentAmount = "%,.2f".format(locale, receivableItem.amountFinal / receivableItem.installments)
            "R$ $amount (${receivableItem.installments}x de R$$installmentAmount)"
        } else {
            val amount = "%,.2f".format(locale, receivableItem.amountOriginal)
            "R$ $amount"
        }

        binding.paymentAmount.text = "Valor: $receivableAmount"

        binding.paymentMethod.text = "Método de pagamento: $paymentMethodName"

        binding.retryAction.setOnClickListener {
            startPayment(orderId, receivableItem)
        }

        binding.closeDialog.setOnClickListener {
            viewModel.abortPayment()
            this.dismiss()
        }

        binding.backSuccessBtn.setOnClickListener {
            this.dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        listener.onResult(result)
        super.onDismiss(dialog)
    }
}