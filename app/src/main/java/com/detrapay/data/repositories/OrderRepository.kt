package com.detrapay.data.repositories

import android.util.Log
import com.detrapay.data.Result
import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderCustomer
import com.detrapay.data.model.OrderItem
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.OrderStatus
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.RefundPaymentData
import com.detrapay.data.model.Simulation
import com.detrapay.data.model.SimulationPayment
import com.detrapay.data.model.VehicleType
import com.detrapay.data.model.remote.OrderCustomerRequest
import com.detrapay.data.model.remote.OrderReceivableRequest
import com.detrapay.data.model.remote.OrderResponse
import com.detrapay.data.model.remote.OrderSimulationItemRequest
import com.detrapay.data.model.remote.OrderSimulationRequest
import com.detrapay.ui.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val detrapayRemoteDataSource: DetrapayRemoteDataSource,
    private val authRepository: AuthRepository
) {

    suspend fun getOrders(forceRefresh: Boolean = false): Result<List<Order>> {
        val user = authRepository.getLoggedUser(true)
        val companyId = user?.companies?.firstOrNull()?.id
        val dispatcherId = user?.dispatchers?.firstOrNull()?.id

        if (companyId == null || dispatcherId == null) {
            return Result.Error(Exception("Usuário não configurado com empresa e despachante."))
        }

        when (val result = detrapayRemoteDataSource.getOrders(companyId, dispatcherId)) {
            is Result.Success -> {
                try {
                    val orders: List<Order?> = result.data.map {
                        try {
                            parseOrder(it)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    return Result.Success(orders.filterNotNull())
                } catch (e: Exception) {
                    Log.e("OrderRepository", "UNABLE TO GET ORDERS: ${e.message}")
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

    suspend fun getOrder(orderId: Int): Result<Order> {
        when (val result = detrapayRemoteDataSource.getOrder(orderId)) {
            is Result.Success -> {
                try {
                    val order = parseOrder(result.data)
                    return Result.Success(order)
                } catch (e: Exception) {
                    Log.e("OrderRepository", "UNABLE TO GET ORDER: ${e.message}")
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
                phoneNumber = orderResponse.attributes.customers.data.attributes.phoneNumber,
                email = orderResponse.attributes.customers.data.attributes.email
            ),
            serviceName = orderResponse.attributes.companies.data.attributes.tradeName,
            creationDate = orderResponse.attributes.createdAt,
            status = OrderStatus.valueOf(orderResponse.attributes.status.uppercase()),
            vehiclePrice = orderResponse.attributes.vehiclePrice,
            billingDate = orderResponse.attributes.billingDate,
            originalAmount = orderResponse.attributes.originalAmount,
            currentAmount = orderResponse.attributes.currentAmount,
            isVehicleFinanced = orderResponse.attributes.isVehicleFinanced,
            isVehicleSpecialPlate = orderResponse.attributes.isSpecialPlate,
            vehicleType = VehicleType(orderResponse.attributes.vehicle_types.data.id, orderResponse.attributes.vehicle_types.data.attributes.name),
            items = orderResponse.attributes.sales_order_items.data.map { item ->
                OrderItem(
                    id = item.id,
                    totalPrice = item.attributes.total_price,
                    discount = item.attributes.discount,
                    salesItemId = item.attributes.sales_item_id,
                    name = item.attributes.sales_items.data.attributes.name,
                    price = item.attributes.unit_price
                )
            },
            receivables = orderResponse.attributes.receivables?.data?.map { receivable ->
                val originalAmount = receivable.attributes.amountOriginal
                val paymentMethod = PaymentMethod(
                    id = receivable.attributes.payment_methods.data.id,
                    name = receivable.attributes.payment_methods.data.attributes.name,
                    maxInstallments = receivable.attributes.payment_methods.data.attributes.max_installments,
                    interestTax = receivable.attributes.payment_methods.data.attributes.interest_tax
                )
                val finalAmount = if (paymentMethod.interestTax != null && paymentMethod.interestTax > 0) {
                    originalAmount + (originalAmount * paymentMethod.interestTax)
                } else {
                    receivable.attributes.amountFinal
                }
                Log.e("OrderRepository", "originalAmount: $originalAmount, interestTax: ${paymentMethod.interestTax}, finalAmount: $finalAmount")


                OrderReceivableItem(
                    id = receivable.id,
                    documentId = receivable.documentId,
                    amountOriginal = originalAmount,
                    amountFinal = finalAmount,
                    max_installments = receivable.attributes.installments,
                    status = OrderReceivableItemStatus.valueOf(receivable.attributes.status.uppercase()),
                    paymentMethod = paymentMethod,
                    paymentDate = receivable.attributes.paymentDate,
                    refundDate = null, // TODO: Add refundDate to response
                    cardLast4 = receivable.attributes.card_last4,
                    cardHolder = receivable.attributes.cardHolder,
                    tax = receivable.attributes.tax,
                    cardBrand = receivable.attributes.cardBrand,
                    authorizationId = null,
                    authorizationCode = receivable.attributes.authorizationCode,
                    pixTxIdCode = null
                )
            } ?: emptyList()
        )
    }

    private fun formatDate(date: String): String {
        val day = date.substring(0, 2)
        val month = date.substring(3, 5)
        val year = date.substring(6, 10)
        return "$year-$month-$day"
    }

    suspend fun createOrder(
        simulation: Simulation,
        simulationPayments: List<SimulationPayment>
    ): Result<Order> {
        val user = authRepository.getLoggedUser(true)
        val salesCompanyId = user?.companies?.firstOrNull()?.id
        val dispatcherId = user?.dispatchers?.firstOrNull()?.id
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
                    .replace(" ", ""),
                amountFinal = simulationPayment.amountFinal
                    .replace("R$", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .replace(" ", ""),
                tax = simulationPayment.paymentMethod.interestTax,
                installments = simulationPayment.installment,
                paymentDate = ""
            )
        }

        when (val result = detrapayRemoteDataSource.createOrder(
            customer = customerRequest,
            simulation = simulationRequest,
            items = simulationItemsRequest,
            receivables = receivablesRequest,
            createdById = user?.id,
            salesCompanyId = salesCompanyId,
            dispatcherId = dispatcherId
        )) {
            is Result.Success -> {
                try {
                    val order = parseOrder(result.data)
                    return Result.Success(order)
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

    suspend fun payOrder(
        orderId:Int,
        receivable: OrderReceivableItem,
        paymentData: PaymentData
    ): Result<Order> {
        val isoDate = formatDate(paymentData.date) + "T" + paymentData.time
        when (val result = detrapayRemoteDataSource.payOrderReceivable(receivable, paymentData.copy(date = isoDate))) {
            is Result.Success -> {
                try {
                    return getOrder(orderId)
                } catch (e: Exception) {
                    Log.e("OrderRepository", "UNABLE TO GET ORDER: ${e.message}")
                    return Result.Error(e)
                }
            }
            is Result.Error -> {
                return result
            }
            else -> {
                return Result.Error(Exception("Tivemos um erro na atualização do pagamento do pedido com nosso servidor, por favor tente novamente."))
            }
        }
    }

    suspend fun refundOrderPayment(orderId:Int, receivable: OrderReceivableItem, refundDate: RefundPaymentData): Result<Order> {
        val isoDate = formatDate(refundDate.date) + "T" + refundDate.time
        when (val result = detrapayRemoteDataSource.refundOrderReceivableItem(receivable.documentId, isoDate)) {
            is Result.Success -> {
                try {
                    return getOrder(orderId)
                } catch (e: Exception) {
                    Log.e("OrderRepository", "UNABLE TO GET ORDER: ${e.message}")
                    return Result.Error(e)
                }
            }
            is Result.Error -> {
                return result
            }
            else -> {
                return Result.Error(Exception("Tivemos um erro no reembolso do pagamento, por favor tente novamente."))
            }
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
}
