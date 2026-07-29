package com.detrapay.ui.home.orders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detrapay.data.Result
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderDocument
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.canBeDeleted
import com.detrapay.data.model.remote.CalculateFeesResponse
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.RegistrationRepository
import com.detrapay.data.repositories.SalesmanRepository
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.PaymentTypeRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import java.io.File
import javax.inject.Inject

data class OrderPaymentRequest(
    val order: Order,
    val paymentMethod: PaymentMethod,
    val amount: Double,
    val amountFinal: Double = amount,
    val installments: Int,
    val idempotencyKey: String = UUID.randomUUID().toString(),
)

data class OrderDocumentsUiState(
    val documents: List<OrderDocument> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val pendingPhotoPath: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val registrationRepository: RegistrationRepository,
    private val salesmanRepository: SalesmanRepository,
) : ViewModel() {

    private var paymentMethods: List<PaymentMethod> = emptyList()
    private val _orderListState = MutableLiveData<UIState<List<Order>>>()
    val orderListState: LiveData<UIState<List<Order>>> = _orderListState
    private val _paymentMethodsState = MutableLiveData<UIState<List<PaymentMethod>>>(UIState.Idle())
    val paymentMethodsState: LiveData<UIState<List<PaymentMethod>>> = _paymentMethodsState
    private val _calculateFeesState = MutableLiveData<UIState<CalculateFeesResponse>>(UIState.Idle())
    val calculateFeesState: LiveData<UIState<CalculateFeesResponse>> = _calculateFeesState
    private val _paymentRecordState = MutableLiveData<UIState<Order>>(UIState.Idle())
    val paymentRecordState: LiveData<UIState<Order>> = _paymentRecordState
    private val _deletePaymentState = MutableLiveData<UIState<Order>>(UIState.Idle())
    val deletePaymentState: LiveData<UIState<Order>> = _deletePaymentState
    private var orderDocumentsSnapshot = OrderDocumentsUiState()
    private val orderDocumentsLock = Any()
    private val _orderDocumentsState = MutableLiveData(orderDocumentsSnapshot)
    val orderDocumentsState: LiveData<OrderDocumentsUiState> = _orderDocumentsState
    private var pendingPhoto: File? = null

    fun loadOrders(forceRefresh: Boolean = false) {
        if (!forceRefresh) _orderListState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = orderRepository.getOrders(forceRefresh)) {
                is Result.Success -> _orderListState.postValue(
                    UIState.Success(OrderPresentation.allOrders(result.data)),
                )
                is Result.Error -> _orderListState.postValue(
                    UIState.Error("Nao foi possivel carregar os pedidos.", result.exception),
                )
            }
        }
    }

    fun loadPaymentMethods(forceRefresh: Boolean = false) {
        _paymentMethodsState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = registrationRepository.loadPaymentMethods(forceRefresh)) {
                is Result.Success -> {
                    paymentMethods = result.data
                    _paymentMethodsState.postValue(UIState.Success(result.data))
                }
                is Result.Error -> _paymentMethodsState.postValue(
                    UIState.Error(
                        result.exception.message ?: "Nao foi possivel carregar os meios de pagamento.",
                        result.exception,
                    ),
                )
            }
        }
    }

    fun availablePaymentMethods(): List<PaymentMethod> = paymentMethods

    fun resolvePaymentMethod(type: String, installments: Int): PaymentMethod? =
        OrderPresentation.exactPaymentMethod(paymentMethods, type, installments)

    fun calculateFees(value: Double, paymentType: String) {
        if (value <= 0.0) {
            _calculateFeesState.postValue(UIState.Error("Informe um valor maior que zero."))
            return
        }
        if (PaymentTypeRules.isDirectNoFeePaymentType(paymentType)) {
            _calculateFeesState.postValue(UIState.Success(PaymentTypeRules.zeroFeeQuote(value, paymentType)))
            return
        }
        _calculateFeesState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = registrationRepository.calculateFees(value, paymentType)) {
                is Result.Success -> _calculateFeesState.postValue(UIState.Success(result.data))
                is Result.Error -> _calculateFeesState.postValue(
                    UIState.Error(
                        result.exception.message ?: "Nao foi possivel calcular as parcelas.",
                        result.exception,
                    ),
                )
            }
        }
    }

    fun clearFeesState() = _calculateFeesState.postValue(UIState.Idle())

    fun recordApprovedOnlinePayment(
        request: OrderPaymentRequest,
        attemptId: String,
        approval: PaymentData,
    ) {
        if (!request.paymentMethod.isOnlinePayment) {
            _paymentRecordState.postValue(UIState.Error("O meio selecionado nao e online."))
            return
        }
        _paymentRecordState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            publishPaymentResult(orderRepository.recordApprovedOnlinePayment(attemptId, approval))
        }
    }

    fun recordOfflinePayment(request: OrderPaymentRequest) {
        if (request.paymentMethod.isOnlinePayment) {
            _paymentRecordState.postValue(UIState.Error("Pagamento online exige aprovacao PagBank."))
            return
        }
        _paymentRecordState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            publishPaymentResult(
                orderRepository.recordOfflinePayment(
                    orderId = request.order.id,
                    paymentMethod = request.paymentMethod,
                    amountOriginal = request.amount,
                    installments = request.installments,
                    idempotencyKey = request.idempotencyKey,
                ),
            )
        }
    }

    private fun publishPaymentResult(result: Result<Order>) {
        when (result) {
            is Result.Success -> _paymentRecordState.postValue(UIState.Success(result.data))
            is Result.Error -> _paymentRecordState.postValue(
                UIState.Error(
                    result.exception.message ?: "Nao foi possivel registrar o pagamento.",
                    result.exception,
                ),
            )
        }
    }

    fun clearPaymentState() = _paymentRecordState.postValue(UIState.Idle())

    fun deleteOfflinePayment(orderId: Int, receivable: OrderReceivableItem) {
        if (!receivable.canBeDeleted()) {
            _deletePaymentState.postValue(
                UIState.Error("Este pagamento nao pode ser excluido."),
            )
            return
        }

        _deletePaymentState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = orderRepository.cancelPendingReceivable(orderId, receivable)) {
                is Result.Success -> _deletePaymentState.postValue(UIState.Success(result.data))
                is Result.Error -> _deletePaymentState.postValue(
                    UIState.Error(
                        result.exception.message ?: "Nao foi possivel excluir o pagamento.",
                        result.exception,
                    ),
                )
            }
        }
    }

    fun clearDeletePaymentState() = _deletePaymentState.postValue(UIState.Idle())

    fun loadOrderDocuments(orderId: Int) {
        updateOrderDocumentsState {
            it.copy(
                isLoading = true,
                errorMessage = null,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = orderRepository.getOrderDocuments(orderId)) {
                is Result.Success -> updateOrderDocumentsState {
                    it.copy(
                        documents = result.data,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                is Result.Error -> updateOrderDocumentsState {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exception.message
                            ?: "Nao foi possivel carregar as fotos do pedido.",
                    )
                }
            }
        }
    }

    fun uploadOrderPhoto(orderId: Int, file: File) {
        if (!file.exists() || file.length() == 0L) {
            updateOrderDocumentsState {
                it.copy(
                    isUploading = false,
                    errorMessage = "A camera nao gerou uma foto valida.",
                )
            }
            return
        }
        if (file.length() > MAX_ORDER_PHOTO_BYTES) {
            file.delete()
            updateOrderDocumentsState {
                it.copy(
                    isUploading = false,
                    pendingPhotoPath = null,
                    errorMessage = "A foto deve ter no maximo 10 MB.",
                )
            }
            return
        }

        pendingPhoto = file
        updateOrderDocumentsState {
            it.copy(
                isUploading = true,
                pendingPhotoPath = file.absolutePath,
                errorMessage = null,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = orderRepository.uploadOrderDocument(orderId, file)) {
                is Result.Success -> {
                    file.delete()
                    pendingPhoto = null
                    updateOrderDocumentsState { current ->
                        current.copy(
                            documents = listOf(result.data) + current.documents.filterNot { document ->
                                document.id == result.data.id
                            },
                            isUploading = false,
                            pendingPhotoPath = null,
                            errorMessage = null,
                        )
                    }
                }
                is Result.Error -> updateOrderDocumentsState {
                    it.copy(
                        isUploading = false,
                        errorMessage = result.exception.message
                            ?: "Nao foi possivel anexar a foto ao pedido.",
                    )
                }
            }
        }
    }

    fun retryOrderPhotoUpload(orderId: Int) {
        pendingPhoto?.let { uploadOrderPhoto(orderId, it) }
    }

    fun discardPendingOrderPhoto() {
        pendingPhoto?.delete()
        pendingPhoto = null
        updateOrderDocumentsState {
            it.copy(
                isUploading = false,
                pendingPhotoPath = null,
                errorMessage = null,
            )
        }
    }

    fun prefetchRegistrationData() {
        viewModelScope.launch(Dispatchers.IO) {
            registrationRepository.loadVehicleTypes()
            salesmanRepository.getSalesmen()
        }
    }

    private fun updateOrderDocumentsState(
        transform: (OrderDocumentsUiState) -> OrderDocumentsUiState,
    ) {
        val updated = synchronized(orderDocumentsLock) {
            transform(orderDocumentsSnapshot).also { orderDocumentsSnapshot = it }
        }
        _orderDocumentsState.postValue(updated)
    }

    private companion object {
        const val MAX_ORDER_PHOTO_BYTES = 10L * 1024L * 1024L
    }
}
