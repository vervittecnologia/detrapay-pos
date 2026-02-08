package com.detrapay.data.repositories

import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.VehicleType
import com.detrapay.data.Result
import com.detrapay.data.model.CustomerSearchData
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderCustomer
import com.detrapay.data.model.OrderItem
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
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
        when (val result = detrapayRemoteDataSource.getVehicleTypes()) {
            is Result.Success -> {
                try {
                    val vehicleTypes = result.data.map {
                        VehicleType(it.id, it.attributes.name)
                    }
                    Logger.d(vehicleTypes.toString())
                    return Result.Success(vehicleTypes)
                } catch (e: Exception) {
                    Logger.d("UNABLE TO LOAD VEHICLE TYPES: ${e.message}")
                    return Result.Error(e)
                }
            }

            is Result.Error -> {
                return result
            }

            else -> {
                return Result.Error(Exception())
            }
        }
    }

    suspend fun loadPaymentMethods(): Result<List<PaymentMethod>> {
        when (val result = detrapayRemoteDataSource.getPaymentMethods()) {
            is Result.Success -> {
                try {
                    val paymentMethods = result.data.map {
                        PaymentMethod(
                            id = it.id,
                            name = it.name,
                            maxInstallments = it.maxInstallments,
                            interestRate = it.interestRate)
                    }
                    Logger.d(paymentMethods.toString())
                    return Result.Success(paymentMethods)
                } catch (e: Exception) {
                    Logger.d("UNABLE TO LOAD PAYMENT METHODS: ${e.message}")
                    return Result.Error(e)
                }
            }

            is Result.Error -> {
                return result
            }

            else -> {
                return Result.Error(Exception())
            }
        }
    }

    suspend fun searchCustomer(cpfCnpj: String): Result<CustomerSearchData> {
        when (val result = detrapayRemoteDataSource.searchCustomer(cpfCnpj)) {
            is Result.Success -> {
                try {
                    result.data.let {
                        val customerSearchData = CustomerSearchData(it.id, it.name, it.whatsapp)
                        Logger.d(customerSearchData.toString())
                        return Result.Success(customerSearchData)
                    }
                } catch (e: Exception) {
                    Logger.d("UNABLE TO LOAD PAYMENT METHODS: ${e.message}")
                    return Result.Error(e)
                }
            }

            is Result.Error -> {
                return result
            }

            else -> {
                return Result.Error(Exception())
            }
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
        specialPlate: Boolean
    ): Result<Simulation> {

        val vehicleValueAmount = vehicleValue.replace("R$", "")
            .replace(" ", "")
            .replace(".", "")
            .replace(",", ".")
            .replace("\\s".toRegex(), "")

        when (val result = detrapayRemoteDataSource.simulate(
            cpfCnpj,
            clientName,
            whatsapp,
            invoiceDate,
            vehicleValueAmount,
            vehicleTypeId,
            disposalVehicle,
            specialPlate
        )) {
            is Result.Success -> {
                try {
                    result.data.let { data ->
                        Logger.d(result.data.toString())
                        val simulationCustomer = SimulationCustomer(
                            data.customer.cpfCnpj,
                            data.customer.name,
                            data.customer.whatsapp
                        )

                        val simulationSimulation = SimulationSimulation(
                            data.simulation.billingDate,
                            data.simulation.vehiclePrice,
                            data.simulation.vehicleDisposal,
                            data.simulation.vehicleSpecialPlate,
                            data.simulation.totalPrice,
                            data.simulation.vehicleTypeId
                        )

                        val simulationItems = data.items.map {
                            SimulationItem(
                                id = it.id,
                                name = it.name,
                                discountAllowed = it.discountAllowed,
                                price = it.price,
                                discount = null
                            )
                        }

                        return Result.Success(
                            Simulation(
                                customer = simulationCustomer,
                                simulation = simulationSimulation,
                                simulationItems = simulationItems
                            )
                        )
                    }
                } catch (e: Exception) {
                    Logger.d("UNABLE TO SIMULATE: ${e.message}")
                    return Result.Error(e)
                }
            }

            is Result.Error -> {
                return result
            }

            else -> {
                return Result.Error(Exception())
            }
        }
    }

    suspend fun createOrder(
        simulation: Simulation,
        simulationPayments: List<SimulationPayment>,
        createdById: String? = null,
        userId: String? = null
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
                    .replace(" ", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .replace("\\s".toRegex(), ""),
                amountFinal = simulationPayment.amountFinal
                    .replace("R$", "")
                    .replace(" ", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .replace("\\s".toRegex(), ""),
                tax = simulationPayment.paymentMethod.interestRate,
                installments = simulationPayment.installment,
                paymentDate = "",
                cpfCnpjCliente = clientCpfCnpj
            )
        }

        when (val result = detrapayRemoteDataSource.createOrder(
            customer = customerRequest,
            simulation = simulationRequest,
            simulationItems = simulationItemsRequest,
            receivables = receivablesRequest,
            createdById = createdById,
            userId = userId
        )) {
            is Result.Success -> {
                try {
                    result.data.let {
                        val order = parseOrder(it)
                        return Result.Success(order)
                    }
                } catch (e: Exception) {
                    Logger.d("UNABLE TO CREATE ORDER: ${e.message}")
                    return Result.Error(e)
                }
            }

            is Result.Error -> {
                return result
            }

            else -> {
                return Result.Error(Exception())
            }
        }
    }

    suspend fun updateOrder(
        orderId: Int,
        simulation: Simulation,
        simulationPayments: List<SimulationPayment>,
        userId: String? = null
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
                    .replace(" ", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .replace("\\s".toRegex(), ""),
                amountFinal = simulationPayment.amountFinal
                    .replace("R$", "")
                    .replace(" ", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .replace("\\s".toRegex(), ""),
                tax = simulationPayment.paymentMethod.interestRate,
                installments = simulationPayment.installment,
                paymentDate = "",
                cpfCnpjCliente = clientCpfCnpj
            )
        }

        when (val result = detrapayRemoteDataSource.updateOrder(
            orderId = orderId,
            customer = customerRequest,
            simulation = simulationRequest,
            simulationItems = simulationItemsRequest,
            receivables = receivablesRequest,
            userId = userId
        )) {
            is Result.Success -> {
                try {
                    result.data.let {
                        val order = parseOrder(it)
                        return Result.Success(order)
                    }
                } catch (e: Exception) {
                    Logger.d("UNABLE TO CREATE ORDER: ${e.message}")
                    return Result.Error(e)
                }
            }

            is Result.Error -> {
                return result
            }

            else -> {
                return Result.Error(Exception())
            }
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

    private fun calculateFinalAmount(finalAmount: String, interestRate: Double?): String {
        val amountFinalStr = finalAmount
            .replace("R$", "")
            .replace(" ", "")
            .replace(".", "")
            .replace(",", ".")
            .replace("\\s".toRegex(), "")

        return try {
            if (interestRate != null && interestRate > 0.0) {
                val amountFinalValue = amountFinalStr.toDouble()
                val bla = amountFinalValue + (amountFinalValue * interestRate)
                "%,.2f".format(locale, bla)
            } else {
                amountFinalStr
            }
        } catch (e:Exception) {
            return amountFinalStr
        }
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
