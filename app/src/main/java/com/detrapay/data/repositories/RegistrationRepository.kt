package com.detrapay.data.repositories

import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.VehicleType
import com.detrapay.data.Result
import com.detrapay.data.model.CustomerSearchData
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderCustomer
import com.detrapay.data.model.OrderStatus
import com.detrapay.data.model.Simulation
import com.detrapay.data.model.SimulationCustomer
import com.detrapay.data.model.SimulationItem
import com.detrapay.data.model.SimulationPayment
import com.detrapay.data.model.SimulationSimulation
import com.detrapay.data.model.remote.OrderCustomerRequest
import com.detrapay.data.model.remote.OrderReceivableRequest
import com.detrapay.data.model.remote.OrderResponse
import com.detrapay.data.model.remote.OrderSimulationItemRequest
import com.detrapay.data.model.remote.OrderSimulationRequest
import com.detrapay.ui.util.Logger
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegistrationRepository @Inject constructor(
    private val detrapayRemoteDataSource: DetrapayRemoteDataSource,
) {

    private val locale = Locale("pt", "BR")

    suspend fun loadVehicleTypes(): Result<List<VehicleType>> {
        return when (val result = detrapayRemoteDataSource.getVehicleTypes()) {
            is Result.Success -> {
                try {
                    val vehicleTypes = result.data.map {
                        VehicleType(it.id, it.attributes.name)
                    }
                    Logger.d(vehicleTypes.toString())
                    Result.Success(vehicleTypes)
                } catch (e: Exception) {
                    Logger.d("UNABLE TO LOAD VEHICLE TYPES: ${e.message}")
                    Result.Error(e)
                }
            }

            is Result.Error -> result
        }
    }

    suspend fun loadPaymentMethods(): Result<List<PaymentMethod>> {
        return when (val result = detrapayRemoteDataSource.getPaymentMethods()) {
            is Result.Success -> {
                try {
                    val paymentMethods = result.data.map {
                        PaymentMethod(
                            id = it.id,
                            name = it.name,
                            maxInstallments = it.maxInstallments,
                            interestTax = it.interestTax)
                    }
                    Logger.d(paymentMethods.toString())
                    Result.Success(paymentMethods)
                } catch (e: Exception) {
                    Logger.d("UNABLE TO LOAD PAYMENT METHODS: ${e.message}")
                    Result.Error(e)
                }
            }

            is Result.Error -> result
        }
    }

    suspend fun searchCustomer(cpfCnpj: String): Result<CustomerSearchData> {
        return when (val result = detrapayRemoteDataSource.searchCustomer(cpfCnpj)) {
            is Result.Success -> {
                try {
                    result.data.let {
                        val customerSearchData = CustomerSearchData(it.id, it.name, it.whatsapp)
                        Logger.d(customerSearchData.toString())
                        Result.Success(customerSearchData)
                    }
                } catch (e: Exception) {
                    Logger.d("UNABLE TO LOAD PAYMENT METHODS: ${e.message}")
                    Result.Error(e)
                }
            }

            is Result.Error -> result
        }
    }

    suspend fun simulate(
        cpfCnpj: String,
        clientName: String,
        whatsapp: String,
        invoiceDate: String,
        vehicleValue: String,
        vehicleTypeId: Int,
        disposalVehicle: Boolean,
        specialPlate: Boolean,
        companyId: Int,
        dispatcherId: Int
    ): Result<Simulation> {

        val vehicleValueAmount = vehicleValue.replace("R$", "")
            .replace(".", "")
            .replace(",", ".")
            .replace("\\s".toRegex(), "")

        return when (val result = detrapayRemoteDataSource.simulate(
            cpfCnpj,
            clientName,
            whatsapp,
            invoiceDate,
            vehicleValueAmount,
            vehicleTypeId,
            disposalVehicle,
            specialPlate,
            companyId,
            dispatcherId
        )) {
            is Result.Success -> {
                try {
                    result.data.let { data ->
                        Logger.d(result.data.toString())
                        val simulationCustomer = SimulationCustomer(
                            data.data.attributes.cpfCnpj,
                            data.data.attributes.name,
                            data.data.attributes.whatsapp
                        )

                        val simulationSimulation = SimulationSimulation(
                            data.data.attributes.billingDate,
                            data.data.attributes.vehiclePrice,
                            data.data.attributes.vehicleDisposal,
                            data.data.attributes.vehicleSpecialPlate,
                            data.data.attributes.totalPrice,
                            data.data.attributes.vehicleTypeId
                        )

                        val simulationItems = data.data.attributes.items.map {
                            SimulationItem(
                                id = it.id,
                                name = it.attributes.name,
                                discountAllowed = it.attributes.discountAllowed,
                                price = it.attributes.price,
                                discount = null
                            )
                        }

                        Result.Success(
                            Simulation(
                                customer = simulationCustomer,
                                simulation = simulationSimulation,
                                simulationItems = simulationItems
                            )
                        )
                    }
                } catch (e: Exception) {
                    Logger.d("UNABLE TO SIMULATE: ${e.message}")
                    Result.Error(e)
                }
            }

            is Result.Error -> result
        }
    }

    suspend fun updateOrder(
        orderId: Int,
        simulation: Simulation,
        simulationPayments: List<SimulationPayment>,
        createdById: String? = null
    ): Result<Order> {
        val clientCpfCnpj = simulation.customer.cpfCnpj.replace(".", "")
            .replace("/", "")
            .replace("-", "")

        val customerRequest = OrderCustomerRequest(
            name = simulation.customer.name,
            cpfCnpj = clientCpfCnpj,
            phoneNumber = simulation.customer.whatsapp.replace("(", "")
                .replace(")", "")
                .replace("-", "")
        )
        val simulationRequest = OrderSimulationRequest(
            billingDate = formatDate(simulation.simulation.billingDate),
            vehiclePrice = simulation.simulation.vehiclePrice,
            vehicleFinanced = simulation.simulation.vehicleDisposal,
            vehicleSpecialPlate = simulation.simulation.vehicleSpecialPlate,
            totalPrice = simulation.simulation.totalPrice.toString(),
            vehicleTypeId = simulation.simulation.vehicleTypeId,
        )
        val simulationItemsRequest = simulation.simulationItems.map { simulationItem ->
            OrderSimulationItemRequest(
                id = simulationItem.id,
                price = calculateItemPrice(simulationItem.price, simulationItem.discount)
            )
        }

        val receivablesRequest = simulationPayments.map { simulationPayment ->
            OrderReceivableRequest(
                paymentMethodId = simulationPayment.paymentMethod.id,
                amountOriginal = simulationPayment.amountOriginal
                    .replace("R$", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .replace("\\s".toRegex(), ""),
                amountFinal = simulationPayment.amountFinal
                    .replace("R$", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .replace("\\s".toRegex(), ""),
                tax = simulationPayment.paymentMethod.interestTax,
                installments = simulationPayment.installment,
                paymentDate = ""
            )
        }

        return when (val result = detrapayRemoteDataSource.updateOrder(
            orderId = orderId,
            customer = customerRequest,
            simulation = simulationRequest,
            items = simulationItemsRequest,
            receivables = receivablesRequest,
            createdById = createdById
        )) {
            is Result.Success -> {
                try {
                    result.data.let {
                        val order = parseOrder(it)
                        Result.Success(order)
                    }
                } catch (e: Exception) {
                    Logger.d("UNABLE TO CREATE ORDER: ${e.message}")
                    Result.Error(e)
                }
            }

            is Result.Error -> result
        }
    }

    private fun parseOrder(orderResponse: OrderResponse): Order {
        return Order(
            id = orderResponse.id,
            customer = OrderCustomer(
                id = orderResponse.attributes.customers.data.id,
                name = orderResponse.attributes.customers.data.attributes.name,
                cpfCnpj = orderResponse.attributes.customers.data.attributes.cpfCnpj,
                phoneNumber = "",
                email = ""
            ),
            serviceName = orderResponse.attributes.companies.data.attributes.tradeName,
            creationDate = orderResponse.attributes.createdAt,
            status = OrderStatus.valueOf(orderResponse.attributes.status.uppercase()),
            vehiclePrice = 0.0,
            billingDate = orderResponse.attributes.billingDate,
            originalAmount = orderResponse.attributes.originalAmount,
            currentAmount = orderResponse.attributes.currentAmount,
            isVehicleFinanced = false,
            isVehicleSpecialPlate = false,
            vehicleType = VehicleType(0, ""),
            items = emptyList(),
            receivables = emptyList()
        )
    }

    private fun calculateItemPrice(itemPrice: Double, discount: Double?): String {
        val price = if (discount != null) {
            itemPrice - discount
        } else {
            itemPrice
        }

        return price.toString()
    }

    private fun formatDate(date: String): String {
        val day = date.substring(0, 2)
        val month = date.substring(3, 5)
        val year = date.substring(6, 10)
        return "$year-$month-$day"
    }
}
