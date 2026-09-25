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
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.local.PendingPaymentCompletion
import com.detrapay.data.model.remote.PaymentAttempt
import com.detrapay.data.repositories.AuthRepository
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.PaymentRepository
import com.detrapay.data.repositories.PendingPaymentRepository
import com.detrapay.ui.home.orders.OrderPaymentRequest
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.PaymentTypeRules
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.Lazy
import kotlin.math.roundToInt

@HiltViewModel
class PaymentDialogViewModel @Inject constructor(
    private val plugPagLazy: Lazy<IPlugPagWrapper>,
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val pendingPaymentRepository: PendingPaymentRepository,
    private val authRepository: AuthRepository,
    private val operationCoordinator: PaymentOperationCoordinator,
) : ViewModel(), PlugPagEventListener {

    private val plugPag by lazy { plugPagLazy.get() }

    private enum class PaymentStep(val message: String) {
        PREPARING("Aguarde, preparando a maquininha."),
        WAITING("Aproxime ou insira seu cartão"),
        PROCESSING("Processando pagamento..."),
        RECORDING("Registrando pagamento no pedido..."),
    }

    private val _paymentState = MutableLiveData<UIState<PaymentData>>()
    val paymentState: LiveData<UIState<PaymentData>> = _paymentState

    @Volatile
    private var lastTerminalMessage: String? = null
    @Volatile
    private var activePaymentIsPix = false
    @Volatile
    private var activeOperationId: String? = null

    fun init() {
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
            ),
        )
        plugPag.setEventListener(this)
    }

    @Synchronized
    fun payOrder(request: OrderPaymentRequest, serial: String) {
        if (!request.paymentMethod.isOnlinePayment) {
            _paymentState.postValue(UIState.Error("Este pagamento deve ser apenas registrado."))
            return
        }

        val confirmedAmountCents = if (request.amountFinal.isFinite()) {
            amountInCents(request.amountFinal)
        } else {
            0
        }
        if (confirmedAmountCents <= 0) {
            finishWithError("O valor confirmado para o pagamento e invalido.")
            return
        }

        if (!operationCoordinator.begin(request.idempotencyKey)) return
        activeOperationId = request.idempotencyKey
        activePaymentIsPix = PaymentTypeRules.normalize(request.paymentMethod.paymentType) == "pix"
        lastTerminalMessage = null
        postStep(PaymentStep.PREPARING)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                init()
            } catch (error: Exception) {
                finishWithError("Nao foi possivel iniciar a maquininha neste dispositivo.", error)
                return@launch
            }
            when (
                val prepared = orderRepository.prepareOnlinePayment(
                    orderId = request.order.id,
                    paymentMethod = request.paymentMethod,
                    amountOriginal = request.amount,
                    installments = request.installments,
                    idempotencyKey = request.idempotencyKey,
                )
            ) {
                is Result.Error -> finishWithError(
                    prepared.exception.message ?: "Nao foi possivel preparar o pagamento online.",
                    prepared.exception,
                )
                is Result.Success -> {
                    val attempt = prepared.data
                    val preparedAmount = attempt.amountFinal
                    if (!preparedAmount.isFinite() ||
                        amountInCents(preparedAmount) != confirmedAmountCents
                    ) {
                        finishWithError(
                            "O backend preparou um total diferente do exibido. " +
                                "Atualize a configuracao antes de tentar novamente.",
                        )
                    } else {
                        val sessionUserId = authRepository.getLoggedUser()?.id
                        if (sessionUserId.isNullOrBlank()) {
                            finishWithError("Nenhum usuario autenticado, contate o suporte.")
                            return@launch
                        }
                        val pending = pendingPaymentRepository.findForAttempt(attempt.id, sessionUserId)
                        if (pending != null) {
                            completeApprovedPayment(pending)
                        } else {
                            startPagBank(request, attempt, sessionUserId, confirmedAmountCents, serial)
                        }
                    }
                }
            }
        }
    }

    @Deprecated("Use o fluxo atomico baseado em OrderPaymentRequest")
    fun payOrder(orderId: Int, receivable: OrderReceivableItem, serial: String) {
        _paymentState.postValue(
            UIState.Error(
                "Este fluxo antigo nao pode iniciar pagamentos. Volte para Pedidos e toque em Pagar.",
            ),
        )
    }

    private suspend fun startPagBank(
        request: OrderPaymentRequest,
        attempt: PaymentAttempt,
        sessionUserId: String,
        amountFinalCents: Int,
        serial: String,
    ) {
        val amountFinal = amountFinalCents / 100.0
        when (val split = orderRepository.updatePaymentAttemptSplitConfig(attempt.id, serial)) {
            is Result.Error -> {
                finishWithError("Nao foi possivel configurar a maquininha: ${split.exception.message}", split.exception)
                return
            }
            is Result.Success -> Unit
        }

        if (!plugPag.isAuthenticated()) {
            finishWithError("Nenhum usuario autenticado, contate o suporte.")
            return
        }

        try {
            if (operationCoordinator.terminalStarted(request.idempotencyKey) !is
                PaymentOperationState.TerminalActive
            ) {
                operationCoordinator.terminalRejected(request.idempotencyKey)
                finishWithError("Pagamento cancelado.")
                return
            }
            if (activePaymentIsPix) {
                _paymentState.postValue(UIState.Loading("Aguardando pagamento via Pix..."))
            } else {
                postStep(PaymentStep.WAITING)
            }
            val result = plugPag.doPayment(
                PlugPagPaymentData(
                    paymentType(request),
                    amountFinalCents,
                    installmentType(request.installments),
                    request.installments,
                    attempt.terminalReference,
                    printReceipt = true,
                    partialPay = false,
                    isCarne = false,
                ),
            )
            if (result.result != PlugPag.RET_OK) {
                if (isAmbiguousCommunicationFailure(result)) {
                    reconcileAmbiguousTransaction(
                        request,
                        attempt,
                        sessionUserId,
                        amountFinalCents,
                        result,
                    )
                } else {
                    saveTransactionLog(request, result, amountFinal)
                    operationCoordinator.terminalRejected(request.idempotencyKey)
                    disposeTerminalSubscriber()
                    finishWithError(terminalFailureMessage(result))
                }
                return
            }

            val approval = paymentData(request, result, amountFinal)
            if (approval.transactionId.isNullOrBlank()) {
                operationCoordinator.terminalApproved(request.idempotencyKey)
                disposeTerminalSubscriber()
                val recovered = runCatching { plugPag.getLastApprovedTransaction() }.getOrNull()
                if (recovered != null && matchesCurrentPayment(request, attempt, amountFinalCents, recovered)) {
                    persistAndCompleteApproval(request, attempt, sessionUserId, recovered, amountFinal)
                } else {
                    finishWithError(
                        "Pagamento aprovado sem identificador. Verifique a venda no PagBank; " +
                            "uma nova cobranca foi bloqueada ate a reconciliacao.",
                        releaseOperation = false,
                    )
                }
                return
            }
            persistAndCompleteApproval(request, attempt, sessionUserId, result, amountFinal)
        } catch (_: PlugPagException) {
            operationCoordinator.terminalRejected(request.idempotencyKey)
            disposeTerminalSubscriber()
            finishWithError("Falha no pagamento.")
        } catch (e: Exception) {
            if (operationCoordinator.currentState() !is
                PaymentOperationState.ApprovedPendingPersistence
            ) {
                operationCoordinator.terminalRejected(request.idempotencyKey)
                disposeTerminalSubscriber()
            }
            finishWithError(e.message ?: "Erro inesperado", e, releaseOperation = false)
        }
    }

    private suspend fun reconcileAmbiguousTransaction(
        request: OrderPaymentRequest,
        attempt: PaymentAttempt,
        sessionUserId: String,
        amountFinalCents: Int,
        failedResult: PlugPagTransactionResult,
    ) {
        val lastApproved = try {
            plugPag.getLastApprovedTransaction()
        } catch (_: Exception) {
            null
        }

        if (lastApproved == null) {
            operationCoordinator.terminalRejected(request.idempotencyKey)
            disposeTerminalSubscriber()
            finishWithError(
                "${terminalFailureCode(failedResult)} - O PagBank nao respondeu e nao foi possivel " +
                    "confirmar a ultima transacao. Verifique a venda no PagBank antes de tentar novamente.",
            )
            return
        }

        if (!matchesCurrentPayment(request, attempt, amountFinalCents, lastApproved)) {
            operationCoordinator.terminalRejected(request.idempotencyKey)
            disposeTerminalSubscriber()
            finishWithError(
                "${terminalFailureCode(failedResult)} - O PagBank nao respondeu. A ultima transacao foi " +
                    "consultada e nenhuma aprovacao deste pedido foi encontrada. Tente novamente em instantes.",
            )
            return
        }

        val amountFinal = amountFinalCents / 100.0
        val recoveredPaymentData = paymentData(request, lastApproved, amountFinal)
        operationCoordinator.terminalApproved(request.idempotencyKey)
        val completion = pendingCompletion(
            request = request,
            attempt = attempt,
            sessionUserId = sessionUserId,
            result = lastApproved,
            paymentData = recoveredPaymentData,
        )
        saveApprovedWithRetry(completion)
        operationCoordinator.persistenceSucceeded(request.idempotencyKey)
        disposeTerminalSubscriber()
        runCatching { saveTransactionLog(request, lastApproved, amountFinal) }
        completeApprovedPayment(completion)
    }

    private suspend fun persistAndCompleteApproval(
        request: OrderPaymentRequest,
        attempt: PaymentAttempt,
        sessionUserId: String,
        result: PlugPagTransactionResult,
        amountFinal: Double,
    ) {
        operationCoordinator.terminalApproved(request.idempotencyKey)
        val approval = paymentData(request, result, amountFinal)
        val completion = pendingCompletion(request, attempt, sessionUserId, result, approval)
        saveApprovedWithRetry(completion)
        operationCoordinator.persistenceSucceeded(request.idempotencyKey)
        disposeTerminalSubscriber()
        runCatching { saveTransactionLog(request, result, amountFinal) }
        completeApprovedPayment(completion)
    }

    private suspend fun saveApprovedWithRetry(completion: PendingPaymentCompletion) {
        var lastFailure: Exception? = null
        repeat(3) { index ->
            try {
                pendingPaymentRepository.saveApproved(completion)
                return
            } catch (error: Exception) {
                lastFailure = error
                if (index < 2) delay(100L * (index + 1))
            }
        }
        throw lastFailure ?: IllegalStateException("Nao foi possivel salvar o pagamento aprovado.")
    }

    private fun matchesCurrentPayment(
        request: OrderPaymentRequest,
        attempt: PaymentAttempt,
        amountFinalCents: Int,
        result: PlugPagTransactionResult,
    ): Boolean {
        if (result.result != PlugPag.RET_OK || result.transactionId.isNullOrBlank()) return false
        if (result.userReference?.trim() != attempt.terminalReference) return false
        if (parseAmountInCents(result.amount) != amountFinalCents) return false
        val recoveredPaymentType = result.paymentType
        return recoveredPaymentType == null || recoveredPaymentType == paymentType(request)
    }

    private fun parseAmountInCents(rawAmount: String?): Int? {
        val digits = rawAmount?.filter(Char::isDigit).orEmpty()
        return digits.toIntOrNull()
    }

    private fun paymentData(
        request: OrderPaymentRequest,
        result: PlugPagTransactionResult,
        amountFinal: Double,
    ) = PaymentData(
        transactionId = result.transactionId,
        transactionCode = result.transactionCode,
        date = result.date,
        time = result.time,
        cardBrand = result.cardBrand,
        cardLast4 = result.holder,
        cardHolder = result.holderName,
        pixTxIdCode = result.pixTxIdCode,
        transactionLog = Gson().toJson(
            mapOf(
                "result" to result.result,
                "errorCode" to result.errorCode,
                "message" to result.message?.take(256),
                "transactionId" to result.transactionId,
                "transactionCode" to result.transactionCode,
                "date" to result.date,
                "time" to result.time,
                "cardBrand" to result.cardBrand,
                "cardLast4" to result.holder?.takeLast(4),
                "pixTxIdCode" to result.pixTxIdCode,
            ),
        ).take(32_768),
        amountOriginal = request.amount,
        amountFinal = amountFinal,
    )

    private suspend fun completeApprovedPayment(completion: PendingPaymentCompletion) {
        if (!operationCoordinator.resumeApproved(completion.idempotencyKey)) return
        activeOperationId = completion.idempotencyKey
        postStep(PaymentStep.RECORDING)
        when (
            val result = orderRepository.recordApprovedOnlinePayment(
                completion.attemptId,
                completion.toPaymentData(),
            )
        ) {
            is Result.Success -> {
                pendingPaymentRepository.deleteCompleted(completion.attemptId)
                operationCoordinator.completionSucceeded(completion.idempotencyKey)
                activeOperationId = null
                _paymentState.postValue(UIState.Success(completion.toPaymentData()))
            }
            is Result.Error -> {
                _paymentState.postValue(
                    UIState.Error(
                        result.exception.message
                            ?: "Pagamento aprovado, mas ainda nao registrado. Tente novamente.",
                        result.exception,
                    ),
                )
            }
        }
    }

    fun resumePendingPayments() {
        viewModelScope.launch(Dispatchers.IO) {
            val sessionUserId = authRepository.getLoggedUser()?.id ?: return@launch
            pendingPaymentRepository.listPending(sessionUserId).forEach { completion ->
                completeApprovedPayment(completion)
            }
        }
    }

    private fun pendingCompletion(
        request: OrderPaymentRequest,
        attempt: PaymentAttempt,
        sessionUserId: String,
        result: PlugPagTransactionResult,
        paymentData: PaymentData,
    ): PendingPaymentCompletion {
        val now = System.currentTimeMillis()
        return PendingPaymentCompletion(
            attemptId = attempt.id,
            sessionUserId = sessionUserId,
            idempotencyKey = request.idempotencyKey,
            terminalReference = attempt.terminalReference,
            orderId = request.order.id,
            transactionId = requireNotNull(paymentData.transactionId),
            transactionCode = paymentData.transactionCode,
            date = paymentData.date,
            time = paymentData.time,
            result = result.result,
            paymentType = paymentType(request),
            installments = request.installments,
            cardBrand = paymentData.cardBrand,
            cardLast4 = paymentData.cardLast4,
            cardHolder = paymentData.cardHolder,
            pixTxIdCode = paymentData.pixTxIdCode,
            transactionLog = paymentData.transactionLog,
            amountOriginal = paymentData.amountOriginal ?: request.amount,
            amountFinal = paymentData.amountFinal ?: attempt.amountFinal,
            status = PendingPaymentCompletion.STATUS_APPROVED,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun PendingPaymentCompletion.toPaymentData() = PaymentData(
        transactionId = transactionId,
        transactionCode = transactionCode,
        date = date,
        time = time,
        cardBrand = cardBrand,
        cardLast4 = cardLast4,
        cardHolder = cardHolder,
        pixTxIdCode = pixTxIdCode,
        transactionLog = transactionLog,
        amountOriginal = amountOriginal,
        amountFinal = amountFinal,
    )

    private suspend fun saveTransactionLog(
        request: OrderPaymentRequest,
        result: PlugPagTransactionResult,
        amountFinal: Double,
    ) {
        paymentRepository.saveTransaction(
            orderId = request.order.id,
            amount = amountFinal,
            installments = request.installments,
            paymentType = request.paymentMethod.name,
            transactionId = result.transactionId,
            transactionCode = result.transactionCode,
            date = result.date,
            result = result.result,
            cardBrand = result.cardBrand,
            cardLast4 = result.holder,
            cardHolder = result.holderName,
            pixTxIdCode = result.pixTxIdCode,
            message = result.message,
            errorCode = result.errorCode,
        )
    }

    private fun paymentType(request: OrderPaymentRequest): Int = when (
        PaymentTypeRules.normalize(request.paymentMethod.paymentType)
    ) {
        "credito" -> PlugPag.TYPE_CREDITO
        "pix" -> PlugPag.TYPE_PIX
        else -> PlugPag.TYPE_DEBITO
    }

    private fun installmentType(installments: Int): Int = if (installments > 1) {
        PlugPag.INSTALLMENT_TYPE_PARC_VENDEDOR
    } else {
        PlugPag.INSTALLMENT_TYPE_A_VISTA
    }

    private fun amountInCents(amount: Double): Int = (amount * 100).roundToInt()

    private fun terminalFailureMessage(result: PlugPagTransactionResult): String {
        val message = result.message?.trim().orEmpty()
        val errorCode = result.errorCode?.trim().orEmpty()
        return when {
            message.isNotBlank() && errorCode.isNotBlank() -> "$errorCode - $message"
            message.isNotBlank() -> message
            errorCode.isNotBlank() -> "Falha no pagamento. Codigo: $errorCode"
            else -> "Falha no pagamento."
        }
    }

    private fun isAmbiguousCommunicationFailure(result: PlugPagTransactionResult): Boolean {
        val errorCode = result.errorCode?.trim()
        return errorCode.equals("A011", ignoreCase = true) ||
            result.result == 1019 || result.result == -1019 ||
            result.result == 1005 || result.result == -1005
    }

    private fun terminalFailureCode(result: PlugPagTransactionResult): String =
        result.errorCode?.trim()?.takeIf(String::isNotBlank) ?: "A011"

    private fun postStep(step: PaymentStep) {
        _paymentState.postValue(UIState.Loading(step.message))
    }

    private fun finishWithError(
        message: String,
        exception: Exception? = null,
        releaseOperation: Boolean = true,
    ) {
        if (releaseOperation) {
            activeOperationId?.let(operationCoordinator::terminalRejected)
            activeOperationId = null
        }
        lastTerminalMessage = null
        _paymentState.postValue(UIState.Error(message, exception))
    }

    fun abortPayment() {
        val operationId = activeOperationId ?: return
        val state = operationCoordinator.requestAbort(operationId)
        if (state !is PaymentOperationState.AbortRequested) return
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { plugPag.abort() }
        }
    }

    override fun onEvent(data: PlugPagEventData) {
        if (!operationCoordinator.acceptsTerminalEvents()) return
        val message = PlugPagEventMessageResolver.resolve(
            data.eventCode,
            data.customMessage,
            activePaymentIsPix,
        )
        if (message == lastTerminalMessage) return
        lastTerminalMessage = message
        viewModelScope.launch(Dispatchers.Main.immediate) {
            if (operationCoordinator.acceptsTerminalEvents()) {
                _paymentState.value = UIState.Loading(message)
            }
        }
    }

    private fun disposeTerminalSubscriber() {
        runCatching { plugPag.disposeSubscriber() }
    }
}
