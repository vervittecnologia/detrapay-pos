package com.detrapay.golden

import android.graphics.Bitmap
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.test.platform.app.InstrumentationRegistry
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.databinding.ActivityOrderDetailsBinding
import com.detrapay.databinding.BottomSheetRegistrationPaymentConfigBinding
import com.detrapay.databinding.PaymentDialogBinding
import com.detrapay.ui.order_details.OrderDetailsPaymentsRecyclerViewAdapter
import com.detrapay.ui.registration.payment_method.InstallmentsAdapter

object PaymentGoldenFixtures {

    private val targetContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    fun renderOrderSummary(): Bitmap = renderOnMainThread {
        val inflater = ViewGoldenRenderer.themedInflater(targetContext)
        val binding = ActivityOrderDetailsBinding.inflate(inflater)

        binding.orderTitleTextView.text = "Pedido #292"
        binding.orderCaptionTextView.text = "Finalizacao de Venda"
        binding.vehicleValueValue.text = "R$ 3.469,18"
        binding.registeredAmountValueTextView.text = "R$ 2.500,00"
        binding.balanceLabelTextView.text = "Saldo devedor"
        binding.balanceValueTextView.text = "R$ 969,18"
        binding.balanceHintTextView.visibility = View.GONE
        binding.receivablesRecyclerView.layoutManager = LinearLayoutManager(targetContext)
        binding.receivablesRecyclerView.layoutParams.height = 2000
        binding.receivablesRecyclerView.adapter = OrderDetailsPaymentsRecyclerViewAdapter(
            values = listOf(
                receivable(
                    id = 1,
                    amount = 2000.0,
                    installments = 10,
                    status = OrderReceivableItemStatus.PENDING,
                    methodName = "Cartao de Credito",
                    paymentType = "credito",
                ),
                receivable(
                    id = 2,
                    amount = 500.0,
                    installments = 1,
                    status = OrderReceivableItemStatus.PAID,
                    methodName = "Dinheiro",
                    paymentType = "dinheiro",
                ),
            ),
            listener = object : OrderDetailsPaymentsRecyclerViewAdapter.OnItemClickListener {
                override fun onItemClick(receivable: OrderReceivableItem) = Unit
                override fun onDeletePendingClick(receivable: OrderReceivableItem) = Unit
            },
        )

        ViewGoldenRenderer.render(binding.root, 706, 1600)
    }

    fun renderPaymentConfigCredit(): Bitmap = renderOnMainThread {
        val inflater = ViewGoldenRenderer.themedInflater(targetContext)
        val binding = BottomSheetRegistrationPaymentConfigBinding.inflate(inflater)

        binding.tvSheetTitle.text = "Confirmar Pagamento"
        binding.tvSheetSubtitle.visibility = View.GONE
        binding.tvAmountLabel.text = "Valor a pagar"
        binding.etPaymentValue.setText("346918")
        binding.tvBrandLabel.visibility = View.GONE
        binding.toggleGroupBrand.visibility = View.GONE
        binding.groupCreditSection.visibility = View.VISIBLE
        binding.rvInstallments.visibility = View.VISIBLE
        binding.rvInstallments.layoutManager = LinearLayoutManager(targetContext)
        binding.rvInstallments.layoutParams.height = 420
        binding.rvInstallments.adapter = InstallmentsAdapter { }
        (binding.rvInstallments.adapter as InstallmentsAdapter).submitList(
            newItems = listOf(
                fee(1, "3.469,18", "3.469,18", true),
                fee(2, "1.734,59", "3.469,18", true),
                fee(5, "693,84", "3.469,18", true),
                fee(10, "346,92", "3.469,18", true),
                fee(12, "315,22", "3.782,64", false),
            ),
            amountOriginal = 3469.18,
        )
        (binding.rvInstallments.adapter as InstallmentsAdapter).setSelected(
            fee(1, "3.469,18", "3.469,18", true),
        )
        binding.cardSimpleSummary.visibility = View.GONE
        binding.tvActionHint.visibility = View.GONE
        binding.btnPrimaryAction.visibility = View.GONE
        binding.btnConfirm.visibility = View.VISIBLE
        binding.btnConfirm.text = "Efetuar Pagamento"

        ViewGoldenRenderer.render(binding.root, 706, 1600)
    }

    fun renderPaymentApproved(): Bitmap = renderOnMainThread {
        val inflater = ViewGoldenRenderer.themedInflater(targetContext)
        val binding = PaymentDialogBinding.inflate(inflater)

        binding.dialogTitle.text = "Pagamento"
        binding.paymentMethod.text = "CARTAO DE CREDITO"
        binding.paymentAmount.text = "R$ 2.184,26"
        binding.paymentInstallments.text = "em 10x de R$ 218,42"
        binding.loadingView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.successView.visibility = View.VISIBLE
        binding.successMessage.text = "Pagamento realizado com sucesso!"
        binding.backSuccessBtn.text = "Voltar ao pedido"

        ViewGoldenRenderer.render(binding.root, 706, 1600)
    }

    private fun renderOnMainThread(block: () -> Bitmap): Bitmap {
        lateinit var bitmap: Bitmap
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            bitmap = block()
        }
        return bitmap
    }

    private fun receivable(
        id: Int,
        amount: Double,
        installments: Int,
        status: OrderReceivableItemStatus,
        methodName: String,
        paymentType: String,
    ): OrderReceivableItem {
        return OrderReceivableItem(
            id = id,
            documentId = id.toString(),
            amountOriginal = amount,
            amountFinal = amount,
            installments = installments,
            status = status,
            paymentMethod = PaymentMethod(
                id = id,
                name = methodName,
                installments = installments,
                interestTax = 0.0,
                paymentType = paymentType,
                isOnlinePayment = paymentType in setOf("credito", "debito", "pix"),
            ),
            paymentDate = "04/10/2025 as 14:00",
            refundDate = null,
            cardLast4 = "5439",
            cardHolder = "John Doe",
            tax = 0.0,
            cardBrand = "VISA",
            authorizationId = null,
            authorizationCode = null,
            pixTxIdCode = null,
        )
    }

    private fun fee(
        installmentNumber: Int,
        installmentValue: String,
        totalValue: String,
        noInterest: Boolean,
    ) = InstallmentFee(
        installmentNumber = installmentNumber,
        installmentValue = installmentValue,
        totalValue = totalValue,
        interestValue = "0,00",
        noInterest = noInterest,
    )
}
