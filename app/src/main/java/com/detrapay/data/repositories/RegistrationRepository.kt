package com.detrapay.data.repositories

import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.VehicleType
import com.detrapay.data.Result
import com.detrapay.data.model.CustomerSearchData
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderCustomer
import com.detrapay.data.model.OrderStatus
import com.detrapay.data.model.Salesman
import com.detrapay.data.model.Simulation
import com.detrapay.data.model.SimulationCustomer
import com.detrapay.data.model.SimulationItem
import com.detrapay.data.model.SimulationPayment
import com.detrapay.data.model.SimulationSimulation
import com.detrapay.data.model.remote.CalculateFeesResponse
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

    private data class CacheEntry<T>(
        val value: T,
        val timestampMs: Long,
    )

    private val locale = Locale("pt", "BR")
    private val catalogTtlMs = 10 * 60 * 1000L
    private var vehicleTypesCache: CacheEntry<List<VehicleType>>? = null
    private var paymentMethodsCache: CacheEntry<List<PaymentMethod>>? = null

    private fun <T> CacheEntry<T>.isValid(ttlMs: Long): Boolean {
        return System.currentTimeMillis() - timestampMs <= ttlMs
    }

    suspend fun calculateFees(
        value: Double,
        paymentType: String,
        brand: String? = null
    ): Result<CalculateFeesResponse> {
        return detrapayRemoteDataSource.calculateFees(value, paymentType, brand)
    }

    suspend fun loadVehicleTypes(forceRefresh: Boolean = false): Result<List<VehicleType>> {
        vehicleTypesCache
            ?.takeIf { !forceRefresh && it.isValid(catalogTtlMs) }
            ?.let { return Result.Success(it.value) }

        return when (val result = detrapayRemoteDataSource.getVehicleTypes()) {
            is Result.Success -> {
                try {
                    val vehicleTypes = result.data.map {
                        VehicleType(it.id, it.name ?: it.attributes?.name.orEmpty())
                    }
                    vehicleTypesCache = CacheEntry(
                        value = vehicleTypes,
                        timestampMs = System.currentTimeMillis()
                    )
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

    suspend fun loadPaymentMethods(forceRefresh: Boolean = false): Result<List<PaymentMethod>> {
        paymentMethodsCache
            ?.takeIf { !forceRefresh && it.isValid(catalogTtlMs) }
            ?.let { return Result.Success(it.value) }

        return when (val result = detrapayRemoteDataSource.getPaymentMethods()) {
            is Result.Success -> {
                try {
                    val paymentMethods = result.data.map {
                        val isOnlinePayment = it.isOnlinePayment ?: throw IllegalStateException(
                            "GET /payment-methods returned method ${it.id} (${it.name}) " +
                                "without required field is_online_payment",
                        )
                        PaymentMethod(
                            id = it.id,
                            name = it.name,
                            installments = it.installments ?: 0,
                            interestTax = it.interestTax,
                            paymentType = it.paymentType,
                            isOnlinePayment = isOnlinePayment,
                        )
                    }
                    paymentMethodsCache = CacheEntry(
                        value = paymentMethods,
                        timestampMs = System.currentTimeMillis()
                    )
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
                price = formatDecimal(simulationItem.price),
                discount = simulationItem.discount?.let(::formatDecimal)
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
        val attributes = orderResponse.attributes
        val customer = orderResponse.customer ?: attributes?.customers?.data?.let {
            com.detrapay.data.model.remote.FlatCustomerResponse(
                id = it.id,
                name = it.attributes.name,
                cpfCnpj = it.attributes.cpfCnpj,
                phoneNumber = it.attributes.phoneNumber,
                email = it.attributes.email
            )
        }
        val salesman = orderResponse.salesman?.let {
            Salesman(id = it.id, name = it.name)
        } ?: attributes?.salesman?.data?.let {
            Salesman(id = it.id, name = it.attributes.name)
        } ?: orderResponse.salesmanName?.takeIf { it.isNotBlank() }?.let {
            Salesman(id = 0, name = it)
        }

        return Order(
            id = orderResponse.id,
            customer = OrderCustomer(
                id = customer?.id ?: 0,
                name = customer?.name ?: orderResponse.customerName.orEmpty(),
                cpfCnpj = customer?.cpfCnpj.orEmpty(),
                phoneNumber = customer?.phoneNumber.orEmpty(),
                email = customer?.email
            ),
            serviceName = orderResponse.company?.tradeName
                ?: attributes?.companies?.data?.attributes?.tradeName
                ?: "",
            creationDate = orderResponse.createdAt ?: attributes?.createdAt.orEmpty(),
            status = runCatching {
                OrderStatus.valueOf((orderResponse.status ?: attributes?.status).orEmpty().uppercase())
            }.getOrDefault(OrderStatus.PENDING),
            vehiclePrice = 0.0,
            billingDate = orderResponse.billingDate ?: attributes?.billingDate.orEmpty(),
            originalAmount = orderResponse.originalAmount ?: attributes?.originalAmount ?: 0.0,
            currentAmount = orderResponse.currentAmount ?: attributes?.currentAmount ?: 0.0,
            isVehicleFinanced = false,
            isVehicleSpecialPlate = false,
            vehicleType = VehicleType(
                orderResponse.vehicleType?.id ?: attributes?.vehicle_types?.data?.id ?: 0,
                orderResponse.vehicleType?.name
                    ?: attributes?.vehicle_types?.data?.attributes?.name
                    ?: orderResponse.vehicleTypeName
                    ?: ""
            ),
            items = emptyList(),
            receivables = emptyList(),
            salesman = salesman
        )
    }

    private fun formatDecimal(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }

    private fun formatDate(date: String): String {
        val day = date.substring(0, 2)
        val month = date.substring(3, 5)
        val year = date.substring(6, 10)
        return "$year-$month-$day"
    }
}
