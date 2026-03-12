package com.detrapay.ui.payment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.uol.pagseguro.plugpagservice.wrapper.IPlugPagWrapper
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagCustomPrinterLayout
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventListener
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import com.detrapay.data.Result
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.PaymentData
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.PaymentRepository
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.PaymentTypeRules
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class PaymentDialogViewModel @Inject constructor(
    private val plugPag: IPlugPagWrapper,
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository
) : ViewModel(), PlugPagEventListener {

    private enum class CardPaymentStep(val message: String) {
        PREPARING("Aguarde, preparando a maquininha."),
        WAITING_CARD("Insira ou aproxime o cartao."),
        PROCESSING("Processando pagamento...")
    }

    private val _paymentState = MutableLiveData<UIState<PaymentData>>()
    val paymentState: LiveData<UIState<PaymentData>> = _paymentState
    @Volatile
    private var terminalPaymentActive = false

    fun init() {
        setupPrintLayout()
        setupPlugPagEventListener()
    }

    private fun setupPlugPagEventListener() {
        plugPag.setEventListener(this)
    }

    private fun setupPrintLayout() {
        plugPag.setPlugPagCustomPrinterLayout(
            PlugPagCustomPrinterLayout(
                "Imprimir via do cliente?",
                "#000000",
                "#FFFFFF",
                "#A0A0A0",
                "#0E5FB2",
                "#000000",
                "#808080",
                "#FFFFFF",
                60,
            )
        )
    }

    fun payOrder(orderId: Int, receivable: OrderReceivableItem, serial: String) {
        terminalPaymentActive = false
        if (isPixPayment(receivable)) {
            _paymentState.postValue(UIState.Loading("Gerando QR Code PIX..."))
            viewModelScope.launch(Dispatchers.IO) {
                when (val result = orderRepository.generatePixCharge(receivable)) {
                    is Result.Success -> {
                        val charge = result.data
                        val paymentData = PaymentData(
                            transactionId = charge.txId,
                            pixTxIdCode = charge.txId,
                            pendingConfirmation = true,
                            pixQrCodeContent = charge.qrCodeContent,
                            pixCopyPasteCode = charge.copyPasteCode,
                            pixQrCodeBase64 = charge.qrCodeBase64,
                            pixExpiresAt = charge.expiresAt
                        )

                        paymentRepository.saveTransaction(
                            orderId = orderId,
                            amount = receivable.amountFinal,
                            installments = receivable.installments,
                            paymentType = receivable.paymentMethod.name,
                            pixTxIdCode = charge.txId,
                            message = "QR Code PIX gerado"
                        )

                        _paymentState.postValue(UIState.Success(paymentData))
                    }

                    is Result.Error -> {
                        _paymentState.postValue(
                            UIState.Error(
                                result.exception.message
                                    ?: "Nao foi possivel gerar o QR Code PIX."
                            )
                        )
                    }
                }
            }
            return
        }

        if (PaymentTypeRules.isDirectNoFeePaymentType(receivable.paymentMethod.paymentType) ||
            PaymentTypeRules.isDirectNoFeePaymentType(receivable.paymentMethod.name)
        ) {
            _paymentState.postValue(
                UIState.Error("Este tipo de pagamento deve ser confirmado manualmente.")
            )
            return
        }

        terminalPaymentActive = true
        postCardLoadingStep(CardPaymentStep.PREPARING)
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val prePayResult = orderRepository.updateSplitConfig(receivable.id, serial)
                if (prePayResult is Result.Error) {
                    terminalPaymentActive = false
                    _paymentState.postValue(
                        UIState.Error(
                            "Nao foi possivel configurar a maquininha: ${prePayResult.exception.message}"
                        )
                    )
                    return@launch
                }

                if (!plugPag.isAuthenticated()) {
                    terminalPaymentActive = false
                    _paymentState.postValue(UIState.Error("Nenhum usuario autenticado, contate o suporte."))
                    return@launch
                }

                val amountInCents = receivable.amountFinal * 100
                val roundedAmountInCents = amountInCents.roundToInt()
                val paymentType = getPaymentType(receivable)
                val installmentType = getInstallmentType(receivable.installments)
                val paymentData = PlugPagPaymentData(
                    paymentType,
                    roundedAmountInCents,
                    installmentType,
                    receivable.installments,
                    null,
                    printReceipt = true,
                    partialPay = false,
                    isCarne = false,
                )

                postCardLoadingStep(CardPaymentStep.WAITING_CARD)
                val plugPagResult: PlugPagTransactionResult = plugPag.doPayment(paymentData)
                val transactionLog = Gson().toJson(plugPagResult)

                if (plugPagResult.result == PlugPag.RET_OK) {
                    val transactionResult = PaymentData(
                        transactionId = plugPagResult.transactionId!!,
                        transactionCode = plugPagResult.transactionCode!!,
                        date = plugPagResult.date!!,
                        time = plugPagResult.time!!,
                        cardBrand = plugPagResult.cardBrand,
                        cardLast4 = plugPagResult.holder,
                        cardHolder = plugPagResult.holderName,
                        pixTxIdCode = plugPagResult.pixTxIdCode,
                        transactionLog = transactionLog,
                        amountOriginal = receivable.amountOriginal,
                        amountFinal = receivable.amountFinal
                    )

                    paymentRepository.saveTransaction(
                        orderId = orderId,
                        amount = receivable.amountFinal,
                        installments = receivable.installments,
                        paymentType = receivable.paymentMethod.name,
                        transactionId = plugPagResult.transactionId,
                        transactionCode = plugPagResult.transactionCode,
                        date = plugPagResult.date,
                        result = plugPagResult.result,
                        cardBrand = plugPagResult.cardBrand,
                        cardLast4 = plugPagResult.holder,
                        cardHolder = plugPagResult.holderName,
                        pixTxIdCode = plugPagResult.pixTxIdCode,
                        message = plugPagResult.message,
                        errorCode = plugPagResult.errorCode
                    )

                    postCardLoadingStep(CardPaymentStep.PROCESSING)
                    when (val apiResult = orderRepository.payOrder(orderId, receivable, transactionResult)) {
                        is Result.Success -> {
                            terminalPaymentActive = false
                            _paymentState.postValue(UIState.Success(transactionResult))
                        }
                        is Result.Error -> {
                            terminalPaymentActive = false
                            _paymentState.postValue(
                                UIState.Error(
                                    apiResult.exception.message ?: "Falha ao concluir pagamento."
                                )
                            )
                        }
                    }
                } else {
                    paymentRepository.saveTransaction(
                        orderId = orderId,
                        amount = receivable.amountFinal,
                        installments = receivable.installments,
                        paymentType = receivable.paymentMethod.name,
                        transactionId = plugPagResult.transactionId,
                        transactionCode = plugPagResult.transactionCode,
                        date = plugPagResult.date,
                        result = plugPagResult.result,
                        cardBrand = plugPagResult.cardBrand,
                        cardLast4 = plugPagResult.holder,
                        cardHolder = plugPagResult.holderName,
                        pixTxIdCode = plugPagResult.pixTxIdCode,
                        message = plugPagResult.message,
                        errorCode = plugPagResult.errorCode
                    )

                    terminalPaymentActive = false
                    _paymentState.postValue(UIState.Error(buildTerminalFailureMessage(plugPagResult)))
                }
            } catch (_: PlugPagException) {
                terminalPaymentActive = false
                _paymentState.postValue(UIState.Error("Falha no pagamento."))
            } catch (e: Exception) {
                terminalPaymentActive = false
                _paymentState.postValue(UIState.Error(e.message ?: "Erro inesperado"))
            }
        }
    }

    private fun buildTerminalFailureMessage(result: PlugPagTransactionResult): String {
        val message = result.message?.trim().orEmpty()
        val errorCode = result.errorCode?.trim().orEmpty()
        return when {
            message.isNotBlank() && errorCode.isNotBlank() -> "$errorCode - $message"
            message.isNotBlank() -> message
            errorCode.isNotBlank() -> "Falha no pagamento. Codigo: $errorCode"
            else -> "Falha no pagamento."
        }
    }

    private fun postCardLoadingStep(step: CardPaymentStep) {
        _paymentState.postValue(UIState.Loading(step.message))
    }

    private fun getInstallmentType(installments: Int): Int {
        return if (installments > 1) {
            PlugPag.INSTALLMENT_TYPE_PARC_VENDEDOR
        } else {
            PlugPag.INSTALLMENT_TYPE_A_VISTA
        }
    }

    private fun isPixPayment(receivable: OrderReceivableItem): Boolean {
        return PaymentTypeRules.isPix(receivable.paymentMethod.paymentType) ||
            PaymentTypeRules.isPix(receivable.paymentMethod.name)
    }

    private fun getPaymentType(receivable: OrderReceivableItem): Int {
        val normalizedType = PaymentTypeRules.normalize(receivable.paymentMethod.paymentType)
        val normalizedName = PaymentTypeRules.normalize(receivable.paymentMethod.name)

        val isCreditCard = normalizedType.contains("credit") ||
            normalizedType.contains("credito") ||
            normalizedName.contains("credit") ||
            normalizedName.contains("credito") ||
            normalizedName.contains("visa") ||
            normalizedName.contains("mastercard")

        val isPix = normalizedType.contains("pix") || normalizedName.contains("pix")

        return when {
            isCreditCard -> PlugPag.TYPE_CREDITO
            isPix -> PlugPag.TYPE_PIX
            else -> PlugPag.TYPE_DEBITO
        }
    }

    fun abortPayment() {
        terminalPaymentActive = false
        viewModelScope.launch(Dispatchers.Default) {
            plugPag.abort()
            plugPag.disposeSubscriber()
        }
    }

    override fun onEvent(data: PlugPagEventData) {
        if (!terminalPaymentActive) return
        val message = data.customMessage.orEmpty().trim().lowercase()
        val step = when {
            message.contains("insira") ||
                message.contains("insere") ||
                message.contains("aproxime") ||
                message.contains("aproximar") ||
                message.contains("passe") ||
                message.contains("cartao") -> CardPaymentStep.WAITING_CARD
            else -> CardPaymentStep.PROCESSING
        }

        postCardLoadingStep(step)
    }
}
