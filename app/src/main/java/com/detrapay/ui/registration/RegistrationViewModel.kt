package com.detrapay.ui.registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.uol.pagseguro.plugpagservice.wrapper.IPlugPagWrapper
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPrinterData
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import com.detrapay.data.Result
import com.detrapay.data.model.CustomerSearchData
import com.detrapay.data.model.LoggedInUser
import com.detrapay.data.model.Order
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.Salesman
import com.detrapay.data.model.Simulation
import com.detrapay.data.model.SimulationItem
import com.detrapay.data.model.SimulationPayment
import com.detrapay.data.model.remote.CalculateFeesResponse
import com.detrapay.data.repositories.AuthRepository
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.RegistrationRepository
import com.detrapay.ui.registration.order_data.OrderData
import com.detrapay.ui.registration.order_data.RegistrationOrderInitialState
import com.detrapay.ui.registration.order_data.RegistrationOrderState
import com.detrapay.ui.registration.payment_method.RegistrationPaymentMethodCreateOrderState
import com.detrapay.ui.registration.payment_method.RegistrationPaymentMethodInitialState
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.detrapay.ui.util.Mask
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val registrationRepository: RegistrationRepository,
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
    private val plugPag: IPlugPagWrapper
) : ViewModel() {
    private val locale = Locale("pt", "BR")

    var inEditMode = false
    var order: Order? = null
    var firstInitialization = true

    private var simulation: Simulation? = null
    private var loggedInUser: LoggedInUser? = null
    private var salesmanId: String? = null

    private var paymentMethods: List<PaymentMethod> = emptyList()

    private var payments: MutableList<SimulationPayment> = mutableListOf()

    private val _paymentsLiveData = MutableLiveData<List<SimulationPayment>>()
    val paymentsLiveData: LiveData<List<SimulationPayment>> = _paymentsLiveData

    private val _remainingBalanceLiveData = MutableLiveData<Double>()
    val remainingBalanceLiveData: LiveData<Double> = _remainingBalanceLiveData

    private val _calculateFeesState = MutableLiveData<UIState<CalculateFeesResponse>>()
    val calculateFeesState: LiveData<UIState<CalculateFeesResponse>> = _calculateFeesState

    private val _registrationState = MutableLiveData<RegistrationState>().apply { 
        value = RegistrationState(currentScreen = 1) 
    }
    val registrationState: LiveData<RegistrationState> = _registrationState

    private val _orderInitialState = MutableLiveData<UIState<RegistrationOrderInitialState>>()
    val orderInitialState: LiveData<UIState<RegistrationOrderInitialState>> = _orderInitialState

    private val _paymentSelectionInitialState =
        MutableLiveData<UIState<RegistrationPaymentMethodInitialState>>()
    val paymentSelectionInitialState: LiveData<UIState<RegistrationPaymentMethodInitialState>> =
        _paymentSelectionInitialState

    private val _paymentSelectionCreateOrderState =
        MutableLiveData<UIState<RegistrationPaymentMethodCreateOrderState>>()
    val paymentSelectionCreateOrderState: LiveData<UIState<RegistrationPaymentMethodCreateOrderState>> =
        _paymentSelectionCreateOrderState

    private val _orderDataClientSearchState = MutableLiveData<UIState<CustomerSearchData>>()
    val orderDataClientSearchState: LiveData<UIState<CustomerSearchData>> =
        _orderDataClientSearchState

    private val _orderDataState = MutableLiveData<UIState<RegistrationOrderState>>()
    val orderDataState: LiveData<UIState<RegistrationOrderState>> = _orderDataState

    private val _orderResumePrintState = MutableLiveData<UIState<String>>()
    val orderResumePrintState: LiveData<UIState<String>> = _orderResumePrintState

    fun initialize(order: Order?) {
        if (order != null) {
            this.order = order
            inEditMode = true
            payments = order.receivables.map {
                SimulationPayment(
                    id = it.id.toLong(),
                    paymentMethod = it.paymentMethod,
                    amountOriginal = "%,.2f".format(locale, it.amountOriginal),
                    amountFinal = "%,.2f".format(locale, it.amountFinal),
                    installment = it.max_installments
                )
            }.toMutableList()
        }
    }

    fun loggedUser(): LoggedInUser? {
        return loggedInUser
    }

    fun loadLoggedUser(){
        viewModelScope.launch(Dispatchers.IO) {
            loggedInUser = authRepository.getLoggedUser(true)
        }
    }

    fun loadOrderScreenContent() {
        Logger.d("RegistrationViewModel - loadScreenContent")
        _orderInitialState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val result = registrationRepository.loadVehicleTypes()
            if (result is Result.Success) {
                if (loggedInUser == null) {
                    loggedInUser = authRepository.getLoggedUser(true)
                }
                val salesmen = loggedInUser?.salesmen ?: emptyList()
                val initialState = if (inEditMode && firstInitialization) {
                    RegistrationOrderInitialState(
                        vehicleTypes = result.data,
                        salesmen = salesmen,
                        orderData = OrderData(
                            cpfCnpj = order!!.customer.cpfCnpj,
                            phone = order!!.customer.phoneNumber,
                            name = order!!.customer.name,
                            invoiceDate = order!!.billingDate,
                            specialPlate = order!!.isVehicleSpecialPlate,
                            disposalVehicle = order!!.isVehicleFinanced,
                            vehicleType = order!!.vehicleType,
                            vehiclePrice = "%,.2f".format(locale, order!!.vehiclePrice),
                            salesmanId = order!!.salesman?.id
                        )
                    )
                } else {
                    RegistrationOrderInitialState(
                        vehicleTypes = result.data,
                        salesmen = salesmen
                    )
                }
                _orderInitialState.postValue(UIState.Success(initialState))
                firstInitialization = false
            } else {
                val error = result as Result.Error
                _orderInitialState.postValue(
                    UIState.Error(
                        message = error.exception.message
                            ?: "Ops! Algo deu errado, tente novamente.",
                        exception = error.exception
                    )
                )
            }
        }
    }

    fun loadPaymentSelectionScreenContent() {
        Logger.d("RegistrationViewModel - loadPaymentSelectionScreenContent")
        _paymentSelectionInitialState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val result = registrationRepository.loadPaymentMethods()
            if (result is Result.Success) {
                paymentMethods = result.data
                _paymentSelectionInitialState.postValue(
                    UIState.Success(
                        RegistrationPaymentMethodInitialState(
                            paymentMethods = paymentMethods,
                            payments = payments,
                        )
                    )
                )
                updatePaymentsList()
            } else {
                val error = result as Result.Error
                _paymentSelectionInitialState.postValue(
                    UIState.Error(
                        message = "Ops! Algo deu errado, tente novamente.",
                        exception = error.exception
                    )
                )
            }
        }
    }

    private fun updatePaymentsList() {
        _paymentsLiveData.postValue(payments.toList())
        calculateRemainingBalance()
    }

    private fun calculateRemainingBalance() {
        val total = totalAmount()
        val paid = paymentsAmountFinal()
        val balance = total - paid
        Logger.d("Remaining Balance: $balance (Total: $total, Paid: $paid)")
        _remainingBalanceLiveData.postValue(balance)
    }

    private fun paymentsAmountFinal(): Double {
        return try {
            payments.fold(0.0) { acc, item ->
                acc + Mask.doubleValue(item.amountOriginal)
            }
        } catch (e: Exception) {
            0.0
        }
    }

    private fun String.normalize(): String {
        val normalized = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
    }

    fun getPaymentMethodsByType(type: String): List<PaymentMethod> {
        val normalizedType = type.normalize()
        val filtered = paymentMethods.filter { 
            val methodType = it.paymentType ?: ""
            methodType.normalize() == normalizedType || it.name.normalize().contains(normalizedType)
        }
        Logger.d("Filtered methods for type $type: ${filtered.size} items found.")
        return filtered
    }

    fun addPaymentByType(type: String) {
        Logger.d("Attempting to add payment of type: $type")
        val methods = getPaymentMethodsByType(type)
        if (methods.isNotEmpty()) {
            val remaining = totalAmount() - paymentsAmount()
            val amountToPay = if (remaining > 0) remaining else 0.0
            
            val method = methods.firstOrNull { it.maxInstallments == 1 } ?: methods.first()
            
            // Calculate final amount including tax for the initial add
            val tax = method.interestTax ?: 0.0
            val amountFinal = amountToPay * (1 + tax)

            val simulationPayment = SimulationPayment(
                id = System.currentTimeMillis(),
                paymentMethod = method,
                amountOriginal = "%,.2f".format(locale, amountToPay),
                amountFinal = "%,.2f".format(locale, amountFinal),
                installment = method.maxInstallments
            )
            payments.add(simulationPayment)
            Logger.d("Payment added. Current payments count: ${payments.size}")
            updatePaymentsList()
        } else {
            Logger.d("No payment methods found for type: $type. Available types: ${paymentMethods.map { it.paymentType }.distinct()}")
        }
    }

    fun onOrderNext(
        cpfCnpj: String,
        clientName: String,
        whatsapp: String,
        invoiceDate: String,
        vehicleValue: String,
        vehicleTypeId: Int,
        disposalVehicle: Boolean,
        specialPlate: Boolean,
        salesmanId: String?
    ) {
        this.salesmanId = salesmanId
        _orderDataState.postValue(UIState.Loading())

        viewModelScope.launch(Dispatchers.IO) {
            if (loggedInUser == null) {
                loggedInUser = authRepository.getLoggedUser(true)
            }

            val companyId = loggedInUser?.companies?.firstOrNull()?.id
            val dispatcherId = loggedInUser?.dispatchers?.firstOrNull()?.id

            if (companyId == null || dispatcherId == null) {
                _orderDataState.postValue(
                    UIState.Error(
                        message = "ID da empresa ou do despachante não encontrado.",
                        exception = Exception("CompanyId or DispatcherId is null")
                    )
                )
                return@launch
            }

            val result = registrationRepository.simulate(
                cpfCnpj,
                clientName,
                whatsapp,
                invoiceDate,
                vehicleValue,
                vehicleTypeId,
                disposalVehicle,
                specialPlate,
                companyId,
                dispatcherId
            )
            if (result is Result.Success) {
                simulation = result.data
                _orderDataState.postValue(UIState.Success(RegistrationOrderState()))
                _registrationState.postValue(RegistrationState(currentScreen = 2))
            } else {
                val error = result as Result.Error
                _orderDataState.postValue(
                    UIState.Error(
                        message = error.exception.message ?: "Erro na simulação.",
                        exception = error.exception
                    )
                )
            }

        }
    }

    fun clearFeesState() {
        _calculateFeesState.value = null
    }

    fun getPaymentById(id: Long): SimulationPayment? {
        return payments.find { it.id == id }
    }

    fun onResumeNext() {
        _registrationState.postValue(RegistrationState(currentScreen = 3))
    }

    fun calculateFees(value: Double, paymentType: String, brand: String) {
        _calculateFeesState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val result = registrationRepository.calculateFees(value, paymentType, brand)
            if (result is Result.Success) {
                _calculateFeesState.postValue(UIState.Success(result.data))
            } else {
                val error = result as Result.Error
                _calculateFeesState.postValue(
                    UIState.Error(
                        message = error.exception.message ?: "Erro ao calcular parcelas.",
                        exception = error.exception
                    )
                )
            }
        }
    }

    fun searchClient(cpfCnpj: String) {
        _orderDataClientSearchState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val result = registrationRepository.searchCustomer(cpfCnpj)
            if (result is Result.Success) {
                _orderDataClientSearchState.postValue(UIState.Success(result.data))
            } else {
                val error = result as Result.Error
                _orderDataClientSearchState.postValue(
                    UIState.Error(
                        message = "Usuário não encontrado.",
                        exception = error.exception
                    )
                )
            }
        }
    }

    fun navigateBack() {
        registrationState.value?.let {
            val currentIndex = it.currentScreen
            if (currentIndex > 1){
                _registrationState.postValue(
                    RegistrationState(
                        currentScreen = currentIndex - 1,
                    )
                )
            }
        }
    }

    private fun totalAmount(): Double {
        simulation?.let { simulation ->
            return simulation.simulationItems.fold(0.0) { acc, item ->
                val price = if (item.discount != null) {
                    item.price - item.discount
                } else {
                    item.price
                }
                acc + price
            }
        }
        return 0.0
    }

    fun simulationTotalAmount(): String {
        val totalAmount = totalAmount()
        val formattedTotalAmount = "%,.2f".format(locale, totalAmount)
        return "R$ $formattedTotalAmount"
    }

    fun simulationItems(): List<SimulationItem> {
        return simulation?.simulationItems ?: emptyList()
    }

    fun simulationItemsWhoSupportDiscount(): List<SimulationItem> {
        return simulationItems().filter { it.discountAllowed == true && it.price > 0 }
    }

    fun canAddDiscount(): Boolean {
        return simulationItemsWhoSupportDiscount().isNotEmpty()
    }

    fun addPayment(item: SimulationPayment) {
        payments.add(item)
        updatePaymentsList()
    }

    fun removePayment(item: SimulationPayment) {
        payments.removeIf { it.id == item.id }
        updatePaymentsList()
    }

    fun updateSimulationPayment(item: SimulationPayment) {
        val index = payments.indexOfFirst { it.id == item.id }
        if (index != -1) {
            payments[index] = item
            updatePaymentsList()
        }
    }

    fun removeDiscount(item: SimulationItem): Int? {
        val itemPosition = simulation?.simulationItems?.indexOf(item)
        val updatedSimulationItems = simulation?.simulationItems?.map {
            if (it.id == item.id) {
                it.copy(discount = null)
            } else {
                it
            }
        }
        simulation =
            simulation?.copy(simulationItems = updatedSimulationItems?.toList() ?: emptyList())

        return itemPosition
    }

    fun addDiscountToSimulationItem(item: SimulationItem?, discountAmount: Double): Int? {
        if (item != null) {
            val itemPosition = simulation?.simulationItems?.indexOf(item)
            val updatedSimulationItems = simulation?.simulationItems?.map {
                if (it.id == item.id) {
                    it.copy(discount = discountAmount)
                } else {
                    it
                }
            }
            simulation =
                simulation?.copy(simulationItems = updatedSimulationItems?.toList() ?: emptyList())
            return itemPosition
        }
        return null
    }

    fun Double.roundTo2DecimalPlacesMath(): Double {
        return Math.round(this * 100.0) / 100.0
    }

    fun createOrder() {
        Logger.d("Creating order with payments: $payments")
        _paymentSelectionCreateOrderState.postValue(UIState.Loading())

        val totalAmount = totalAmount()
        val roundedTotalAmount = totalAmount.roundTo2DecimalPlacesMath()

        val paymentsAmount = paymentsAmount()
        val roundedPaymentsAmount = paymentsAmount.roundTo2DecimalPlacesMath()

        Logger.d("Validation check - Expected (Total): $roundedTotalAmount, Calculated (Payments Sum): $roundedPaymentsAmount")
        
        if (roundedTotalAmount != roundedPaymentsAmount) {
            val errorMsg = if (payments.count() > 1) {
                "A soma dos pagamentos (R$ %,.2f) deve ser igual ao valor total do pedido (R$ %,.2f).".format(locale, paymentsAmount, totalAmount)
            } else {
                "O valor do pagamento (R$ %,.2f) deve ser igual ao valor total do pedido (R$ %,.2f).".format(locale, paymentsAmount, totalAmount)
            }
            _paymentSelectionCreateOrderState.postValue(UIState.Error(errorMsg))
            return
        }

        if (simulation != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val result = orderRepository.createOrder(
                        simulation = simulation!!,
                        simulationPayments = payments,
                        salesmanId = salesmanId
                    )
                    if (result is Result.Success) {
                        Logger.d("Order created successfully: ${result.data.id}")
                        _paymentSelectionCreateOrderState.postValue(
                            UIState.Success(
                                RegistrationPaymentMethodCreateOrderState(result.data)
                            )
                        )
                    } else {
                        val error = result as Result.Error
                        Logger.d("Repository returned error: ${error.exception.message}")
                        _paymentSelectionCreateOrderState.postValue(
                            UIState.Error(
                                message = error.exception.message
                                    ?: "Erro ao processar pedido no servidor. Tente novamente.",
                                exception = error.exception
                            )
                        )
                    }
                } catch (e: Exception) {
                    Logger.d("Crash during createOrder: ${e.message}")
                    _paymentSelectionCreateOrderState.postValue(UIState.Error("Erro inesperado: ${e.message}"))
                }
            }
        } else {
            _paymentSelectionCreateOrderState.postValue(UIState.Error("Dados incompletos, por favor verificar etapas anteriores."))
        }
    }

    private fun paymentsAmount(): Double {
        return try {
            payments.fold(0.0) { acc, item ->
                acc + Mask.doubleValue(item.amountOriginal)
            }
        } catch (e: Exception) {
            0.0
        }
    }

    fun printOrderResume(path: String){
        _orderResumePrintState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val result = plugPag.printFromFile(
                    printerData = PlugPagPrinterData(
                        path,
                        100,
                        PlugPag.MIN_PRINTER_STEPS
                    )
                )
                if (result.result == PlugPag.RET_OK) {
                    _orderResumePrintState.postValue(UIState.Success("Impressão realizada com sucesso!"))
                } else {
                    _orderResumePrintState.postValue(UIState.Error(result.errorCode + result.message))
                }
            } catch (e: PlugPagException) {
                _orderResumePrintState.postValue(UIState.Error(e.message ?: "Falha na impressão!"))
            }
        }
    }
}
