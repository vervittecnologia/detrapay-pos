package com.detrapay.ui.registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.uol.pagseguro.plugpagservice.wrapper.IPlugPagWrapper
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPrinterData
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import com.detrapay.BuildConfig
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
import com.detrapay.data.repositories.SalesmanRepository
import com.detrapay.debug.DebugOrderDefaults
import com.detrapay.ui.registration.order_data.OrderData
import com.detrapay.ui.registration.order_data.RegistrationOrderInitialState
import com.detrapay.ui.registration.order_data.RegistrationOrderState
import com.detrapay.ui.registration.payment_method.RegistrationPaymentMethodCreateOrderState
import com.detrapay.ui.registration.payment_method.RegistrationPaymentMethodInitialState
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.detrapay.ui.util.Mask
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.inject.Inject

private data class FeesCacheKey(
    val value: Double,
    val paymentType: String,
    val brand: String,
)

data class WhatsAppSharePayload(
    val phone: String,
    val formattedPhone: String,
    val message: String,
    val waMeLink: String,
)

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val registrationRepository: RegistrationRepository,
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
    private val salesmanRepository: SalesmanRepository,
    private val plugPag: IPlugPagWrapper
) : ViewModel() {
    private val locale = Locale("pt", "BR")

    var inEditMode = false
    var order: Order? = null
    var firstInitialization = true

    private var simulation: Simulation? = null
    private var loggedInUser: LoggedInUser? = null
    private var salesmanId: Int? = null
    private var salesmen: List<Salesman> = emptyList()

    private var paymentMethods: List<PaymentMethod> = emptyList()
    private val feesCache = mutableMapOf<FeesCacheKey, CalculateFeesResponse>()

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
                    installment = it.installments
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

    private var vehicleTypes: List<com.detrapay.data.model.VehicleType> = emptyList()

    fun getVehicleTypeName(id: Int): String {
        return vehicleTypes.find { it.id == id }?.name ?: "ID: $id"
    }

    fun getSalesmanName(): String {
        return salesmen.find { it.id == salesmanId }?.name ?: "N/A"
    }

    fun getDealershipName(): String {
        return loggedInUser?.companies?.firstOrNull()?.name ?: "N/A"
    }

    fun simulationSimulation() = simulation?.simulation

    fun loadOrderScreenContent() {
        Logger.d("RegistrationViewModel - loadScreenContent")
        _orderInitialState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val vehicleTypesDeferred = async { registrationRepository.loadVehicleTypes() }
            val salesmenDeferred = async { salesmanRepository.getSalesmen() }

            val result = vehicleTypesDeferred.await()
            if (result is Result.Success) {
                vehicleTypes = result.data
                if (loggedInUser == null) {
                    loggedInUser = authRepository.getLoggedUser(false)
                }
                val salesmenResult = salesmenDeferred.await()
                if (salesmenResult is Result.Error) {
                    _orderInitialState.postValue(
                        UIState.Error(
                            message = salesmenResult.exception.message
                                ?: "Ops! Algo deu errado ao carregar vendedores.",
                            exception = salesmenResult.exception
                        )
                    )
                    return@launch
                }
                salesmen = (salesmenResult as Result.Success).data
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
                        salesmen = salesmen,
                        orderData = if (BuildConfig.DEBUG) {
                            DebugOrderDefaults.createOrderData(
                                vehicleTypes = result.data,
                                salesmen = salesmen
                            )
                        } else {
                            null
                        }
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
        val balance = (total - paid).roundTo2DecimalPlacesMath()
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
            
            val method = methods.firstOrNull { it.installments == 1 } ?: methods.first()
            
            // Calculate final amount including tax for the initial add
            val tax = method.interestTax ?: 0.0
            val amountFinal = amountToPay * (1 + tax)

            val simulationPayment = SimulationPayment(
                id = System.currentTimeMillis(),
                paymentMethod = method,
                amountOriginal = "%,.2f".format(locale, amountToPay),
                amountFinal = "%,.2f".format(locale, amountFinal),
                installment = method.installments
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
        salesmanId: Int?
    ) {
        this.salesmanId = salesmanId
        _orderDataState.postValue(UIState.Loading())

        viewModelScope.launch(Dispatchers.IO) {
            if (loggedInUser == null) {
                loggedInUser = authRepository.getLoggedUser(false)
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
        _calculateFeesState.value = UIState.Idle()
    }

    fun getPaymentById(id: Long): SimulationPayment? {
        return payments.find { it.id == id }
    }

    fun getCachedFees(value: Double, paymentType: String, brand: String): CalculateFeesResponse? {
        return feesCache[feesCacheKey(value, paymentType, brand)]
    }

    fun onResumeNext() {
        _registrationState.postValue(RegistrationState(currentScreen = 3))
    }

    fun calculateFees(value: Double, paymentType: String, brand: String) {
        _calculateFeesState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val result = registrationRepository.calculateFees(value, paymentType, brand)
            if (result is Result.Success) {
                feesCache[feesCacheKey(value, paymentType, brand)] = result.data
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

    private fun feesCacheKey(value: Double, paymentType: String, brand: String): FeesCacheKey {
        return FeesCacheKey(
            value = value.roundTo2DecimalPlacesMath(),
            paymentType = paymentType.trim().lowercase(),
            brand = brand.trim().uppercase(),
        )
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

    fun buildWhatsAppTargetPhone(rawPhone: String): String {
        val digits = rawPhone.filter(Char::isDigit)
        if (digits.isBlank()) return ""

        return when {
            digits.startsWith("55") -> digits
            digits.length == 11 -> "55$digits"
            else -> digits
        }
    }

    fun formatWhatsAppTargetPhone(rawPhone: String): String {
        val phone = buildWhatsAppTargetPhone(rawPhone)
        if (phone.isBlank()) return ""

        return when {
            phone.length == 13 && phone.startsWith("55") -> {
                "+${phone.substring(0, 2)} (${phone.substring(2, 4)}) ${phone.substring(4, 9)}-${phone.substring(9)}"
            }
            phone.length == 12 && phone.startsWith("55") -> {
                "+${phone.substring(0, 2)} (${phone.substring(2, 4)}) ${phone.substring(4, 8)}-${phone.substring(8)}"
            }
            else -> phone
        }
    }

    fun buildWhatsAppMessage(): String? {
        val currentSimulation = simulation ?: return null
        val simulationData = currentSimulation.simulation
        val customer = currentSimulation.customer
        val vehiclePrice = formatCurrency(Mask.toSafeDouble(simulationData.vehiclePrice))

        return buildList {
            add("Resumo do pedido")
            add("Cliente: ${customer.name}")
            add("CPF/CNPJ: ${customer.cpfCnpj}")
            add("WhatsApp: ${customer.whatsapp}")
            add("Concessionaria: ${getDealershipName()}")
            add("Vendedor: ${getSalesmanName()}")
            add("Tipo de veiculo: ${getVehicleTypeName(simulationData.vehicleTypeId)}")
            add("Valor do veiculo: $vehiclePrice")
            add("Data de aquisicao: ${simulationData.billingDate}")
            add("Itens:")

            currentSimulation.simulationItems.forEach { item ->
                add("- ${item.name}: ${formatCurrency(item.price)}")
                item.discount?.takeIf { it > 0 }?.let { discount ->
                    add("Desconto: -${formatCurrency(discount)}")
                }
            }

            add("Valor total final: ${simulationTotalAmount()}")
        }.joinToString(separator = "\n")
    }

    fun buildWhatsAppWaMeLink(): String? {
        val message = buildWhatsAppMessage() ?: return null
        val phone = buildWhatsAppTargetPhone(simulation?.customer?.whatsapp.orEmpty())
        if (phone.isBlank()) return null

        val encodedMessage = URLEncoder.encode(
            message,
            StandardCharsets.UTF_8.toString()
        ).replace("+", "%20")

        return "https://wa.me/$phone?text=$encodedMessage"
    }

    fun buildWhatsAppSharePayload(): WhatsAppSharePayload? {
        val message = buildWhatsAppMessage() ?: return null
        val phone = buildWhatsAppTargetPhone(simulation?.customer?.whatsapp.orEmpty())
        val waMeLink = buildWhatsAppWaMeLink() ?: return null
        if (phone.isBlank()) return null

        return WhatsAppSharePayload(
            phone = phone,
            formattedPhone = formatWhatsAppTargetPhone(phone),
            message = message,
            waMeLink = waMeLink
        )
    }

    private fun supportsDiscount(item: SimulationItem): Boolean {
        return item.discountAllowed && item.price > 0
    }

    fun simulationItemsWhoSupportDiscount(): List<SimulationItem> {
        return simulationItems().filter { item ->
            supportsDiscount(item) && (item.discount ?: 0.0) <= 0.0
        }
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
        updateSimulationItems(updatedSimulationItems)

        return itemPosition
    }

    fun addDiscountToSimulationItem(item: SimulationItem?, discountAmount: Double): Int? {
        if (item != null && supportsDiscount(item) && discountAmount > 0 && discountAmount <= item.price) {
            val itemPosition = simulation?.simulationItems?.indexOf(item)
            val updatedSimulationItems = simulation?.simulationItems?.map {
                if (it.id == item.id) {
                    it.copy(discount = discountAmount)
                } else {
                    it
                }
            }
            updateSimulationItems(updatedSimulationItems)
            return itemPosition
        }
        return null
    }

    private fun updateSimulationItems(updatedSimulationItems: List<SimulationItem>?) {
        val currentSimulation = simulation ?: return
        val newItems = updatedSimulationItems?.toList() ?: emptyList()
        simulation = currentSimulation.copy(
            simulation = currentSimulation.simulation.copy(
                totalPrice = newItems.fold(0.0) { acc, item ->
                    acc + (item.price - (item.discount ?: 0.0))
                }.roundTo2DecimalPlacesMath()
            ),
            simulationItems = newItems
        )
        calculateRemainingBalance()
    }

    fun Double.roundTo2DecimalPlacesMath(): Double {
        return Math.round(this * 100.0) / 100.0
    }

    private fun formatCurrency(value: Double): String {
        return "R$ %,.2f".format(locale, value)
    }

    fun createOrder() {
        submitOrder(payments.toList())
    }

    fun createOrderWithoutPayments() {
        submitOrder(emptyList())
    }

    private fun submitOrder(paymentsToCreate: List<SimulationPayment>) {
        Logger.d("Creating order with payments: $paymentsToCreate")
        _paymentSelectionCreateOrderState.postValue(UIState.Loading())
        if (simulation != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val result = orderRepository.createOrder(
                        simulation = simulation!!,
                        simulationPayments = paymentsToCreate,
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
