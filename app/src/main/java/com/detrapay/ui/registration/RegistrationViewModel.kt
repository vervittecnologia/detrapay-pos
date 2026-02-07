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
import com.detrapay.data.model.Simulation
import com.detrapay.data.model.SimulationItem
import com.detrapay.data.model.SimulationPayment
import com.detrapay.data.repositories.AuthRepository
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
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val registrationRepository: RegistrationRepository,
    private val authRepository: AuthRepository,
    private val plugPag: IPlugPagWrapper
) : ViewModel() {
    private val locale = Locale("pt", "BR")

    var inEditMode = false
    var order: Order? = null
    var firstInitialization = true

    private var simulation: Simulation? = null
    private var loggedInUser: LoggedInUser? = null

    private var paymentMethods: List<PaymentMethod> = emptyList()

    private var payments: MutableList<SimulationPayment> =
        emptyList<SimulationPayment>().toMutableList()

    private val _registrationState = MutableLiveData<RegistrationState>()
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

    fun loadOrderScreenContent() {
        Logger.d("RegistrationViewModel - loadScreenContent")
        _orderInitialState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val result = registrationRepository.loadVehicleTypes()
            if (result is Result.Success) {
                val initialState = if (inEditMode && firstInitialization) {
                    RegistrationOrderInitialState(
                        vehicleTypes = result.data,
                        orderData = OrderData(
                            cpfCnpj = order!!.customer.cpfCnpj,
                            phone = order!!.customer.phoneNumber,
                            name = order!!.customer.name,
                            invoiceDate = order!!.billingDate,
                            specialPlate = order!!.isVehicleSpecialPlate,
                            disposalVehicle = order!!.isVehicleFinanced,
                            vehicleType = order!!.vehicleType,
                            vehiclePrice = "%,.2f".format(locale, order!!.vehiclePrice),
                        )
                    )
                } else {
                    RegistrationOrderInitialState(
                        vehicleTypes = result.data
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
                if (payments.isEmpty()) {
                    val paymentMethod = paymentMethods.first()
                    val simulationPayment = SimulationPayment(
                        id = 0,
                        paymentMethod = paymentMethod,
                        amountOriginal = "",
                        amountFinal = "",
                        installment = paymentMethod.maxInstallments
                    )
                    payments.add(simulationPayment)
                }
                _paymentSelectionInitialState.postValue(
                    UIState.Success(
                        RegistrationPaymentMethodInitialState(
                            paymentMethods = paymentMethods,
                            payments = payments,
                        )
                    )
                )
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

    fun onOrderNext(
        cpfCnpj: String,
        clientName: String,
        whatsapp: String,
        invoiceDate: String,
        vehicleValue: String,
        vehicleTypeId: Int,
        disposalVehicle: Boolean,
        specialPlate: Boolean
    ) {
        _orderDataState.postValue(UIState.Loading())

        viewModelScope.launch(Dispatchers.IO) {
            val result = registrationRepository.simulate(
                cpfCnpj,
                clientName,
                whatsapp,
                invoiceDate,
                vehicleValue,
                vehicleTypeId,
                disposalVehicle,
                specialPlate
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

    fun onResumeNext() {
        _registrationState.postValue(RegistrationState(currentScreen = 3))
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
            return simulation.simulationItems.sumOf {
                if (it.discount != null) {
                    it.price - it.discount
                } else {
                    it.price
                }
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

    fun removePayment(item: SimulationPayment) {
        try {
            payments.removeIf { it.id == item.id }
        } catch (e: Exception) {
            Logger.d(e.message ?: "")
        }
    }

    fun addPayment(item: SimulationPayment) {
        payments.add(item)
    }

    fun updateSimulationPayment(item: SimulationPayment) {
        try {
            val paymentIndex = payments.indexOfFirst { it.id == item.id }
            payments[paymentIndex] = item
        } catch (e: Exception) {
            Logger.d("updateSimulationPayment: ${e.message}")
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
        Logger.d(payments.toString())
        _paymentSelectionCreateOrderState.postValue(UIState.Loading())

        val totalAmount = totalAmount()
        val roundedTotalAmount = totalAmount.roundTo2DecimalPlacesMath()

        val paymentsAmount = paymentsAmount()
        val roundedPaymentsAmount = paymentsAmount.roundTo2DecimalPlacesMath()

        Logger.d("Expected: ${roundedTotalAmount}, Calculated: ${roundedPaymentsAmount}")
        if (roundedTotalAmount != roundedPaymentsAmount) {
            if (payments.count() > 1) {
                _paymentSelectionCreateOrderState.postValue(UIState.Error("A soma dos pagamentos deve ser igual ao valor total do pedido."))
            } else {
                _paymentSelectionCreateOrderState.postValue(UIState.Error("O valor do pagamento deve ser igual ao valor total do pedido."))
            }
            return
        }

        if (simulation != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val user = authRepository.getLoggedUser(true)
                val result = if (inEditMode) {
                    registrationRepository.updateOrder(
                        orderId = order!!.id,
                        simulation = simulation!!,
                        simulationPayments = payments,
                        userId = user?.preferredEmployeeId
                    )
                } else {
                    registrationRepository.createOrder(
                        simulation = simulation!!,
                        simulationPayments = payments,
                        createdById = user?.id,
                        userId = user?.preferredEmployeeId
                    )
                }
                if (result is Result.Success) {
                    _paymentSelectionCreateOrderState.postValue(
                        UIState.Success(
                            RegistrationPaymentMethodCreateOrderState(result.data)
                        )
                    )
                } else {
                    val error = result as Result.Error
                    _paymentSelectionCreateOrderState.postValue(
                        UIState.Error(
                            message = error.exception.message
                                ?: "Ops! Algo deu errado, tente novamente.",
                            exception = error.exception
                        )
                    )
                }
            }
        } else {
            _paymentSelectionCreateOrderState.postValue(UIState.Error("Dados incompletos, por favor verificar etapas anteriores."))
        }
    }

    private fun paymentsAmount(): Double {
        return try {
            payments.sumOf {
                it.amountOriginal.replace("R$", "", true)
                    .replace(" ", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .replace("\\s".toRegex(), "")
                    .toDouble()
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