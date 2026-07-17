package com.detrapay.data.repositories

import android.util.Log
import com.detrapay.BuildConfig
import com.detrapay.data.Result
import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderCustomer
import com.detrapay.data.model.OrderItem
import com.detrapay.data.model.OrderReceivable
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.OrderStatus
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.PixCharge
import com.detrapay.data.model.RefundPaymentData
import com.detrapay.data.model.Salesman
import com.detrapay.data.model.Simulation
import com.detrapay.data.model.SimulationPayment
import com.detrapay.data.model.VehicleType
import com.detrapay.data.model.canBeDeleted
import com.detrapay.data.model.remote.CreateOrderPaymentRequest
import com.detrapay.data.model.remote.CreateOrderRequest
import com.detrapay.data.model.remote.CreateOrderSimulationRequest
import com.detrapay.data.model.remote.OrderCustomerRequest
import com.detrapay.data.model.remote.OrderReceivableRequest
import com.detrapay.data.model.remote.OrderResponse
import com.detrapay.data.model.remote.OrderSimulationItemRequest
import com.detrapay.data.model.remote.OrderSimulationRequest
import com.detrapay.data.model.remote.SplitConfigRequest
import com.detrapay.ui.util.Logger
import com.detrapay.ui.util.Mask
import com.detrapay.ui.util.DebugConstants
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val detrapayRemoteDataSource: DetrapayRemoteDataSource,
    private val authRepository: AuthRepository
) {

    private data class CacheEntry<T>(
        val value: T,
        val timestampMs: Long,
    )

    private val ordersCacheTtlMs = 30 * 1000L
    private var ordersCache: CacheEntry<List<Order>>? = null
    private val orderDetailsCache = mutableMapOf<Int, Order>()

    private fun <T> CacheEntry<T>.isValid(ttlMs: Long): Boolean {
        return System.currentTimeMillis() - timestampMs <= ttlMs
    }

    private fun updateOrdersCache(orders: List<Order>) {
        ordersCache = CacheEntry(
            value = orders,
            timestampMs = System.currentTimeMillis()
        )
    }

    private fun invalidateOrdersCache() {
        ordersCache = null
    }

    suspend fun getOrders(forceRefresh: Boolean = false): Result<List<Order>> {
        ordersCache
            ?.takeIf { !forceRefresh && it.isValid(ordersCacheTtlMs) }
            ?.let { return Result.Success(it.value) }

        val user = authRepository.getLoggedUser(false)
        val companyId = user?.companies?.firstOrNull()?.id
        val dispatcherId = user?.dispatchers?.firstOrNull()?.id

        if (companyId == null || dispatcherId == null) {
            return Result.Error(Exception("Usuário não configurado com empresa e despachante."))
        }

        when (val result = detrapayRemoteDataSource.getOrders(companyId, dispatcherId)) {
            is Result.Success -> {
                try {
                    val orders: List<Order?> = result.data.map { orderResponse ->
                        try {
                            parseOrder(orderResponse)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val parsedOrders = orders.filterNotNull()
                    updateOrdersCache(parsedOrders)
                    return Result.Success(parsedOrders)
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

    suspend fun getOrder(orderId: Int, forceRefresh: Boolean = false): Result<Order> {
        orderDetailsCache[orderId]
            ?.takeIf { !forceRefresh }
            ?.let { return Result.Success(it) }

        when (val result = detrapayRemoteDataSource.getOrder(orderId)) {
            is Result.Success -> {
                try {
                    val order = parseOrder(result.data)
                    orderDetailsCache[orderId] = order
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

    suspend fun getReceivables(forceRefresh: Boolean = false): Result<List<OrderReceivable>> {
        return when (val ordersResult = getOrders(forceRefresh)) {
            is Result.Success -> {
                val receivables = mutableListOf<OrderReceivable>()

                for (orderSummary in ordersResult.data) {
                    val order = when (val orderResult = getOrder(orderSummary.id, forceRefresh)) {
                        is Result.Success -> orderResult.data
                        is Result.Error -> return Result.Error(orderResult.exception)
                    }
                    receivables += order.receivables.map { receivable ->
                        OrderReceivable(order = order, receivable = receivable)
                    }
                }

                Result.Success(
                    receivables.sortedWith(
                        compareByDescending<OrderReceivable> { it.receivable.status == OrderReceivableItemStatus.PENDING }
                            .thenByDescending { it.order.id }
                    )
                )
            }
            is Result.Error -> ordersResult
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
        val companyTradeName = orderResponse.company?.tradeName
            ?: attributes?.companies?.data?.attributes?.tradeName
            ?: ""
        val vehicleTypeId = orderResponse.vehicleType?.id
            ?: attributes?.vehicle_types?.data?.id
            ?: 0
        val vehicleTypeName = orderResponse.vehicleType?.name
            ?: attributes?.vehicle_types?.data?.attributes?.name
            ?: orderResponse.vehicleTypeName
            ?: ""
        val salesman = orderResponse.salesman?.let {
            Salesman(id = it.id, name = it.name)
        } ?: attributes?.salesman?.data?.let {
            Salesman(id = it.id, name = it.attributes.name)
        } ?: orderResponse.salesmanName?.takeIf { it.isNotBlank() }?.let {
            Salesman(id = 0, name = it)
        }
        val items = orderResponse.items?.map { item ->
            OrderItem(
                id = item.id,
                totalPrice = item.totalPrice ?: 0.0,
                discount = item.discount ?: 0.0,
                salesItemId = item.salesItemId,
                name = item.name,
                price = item.unitPrice
            )
        } ?: attributes?.sales_order_items?.data?.map { item ->
            OrderItem(
                id = item.id,
                totalPrice = item.attributes.total_price,
                discount = item.attributes.discount,
                salesItemId = item.attributes.sales_item_id,
                name = item.attributes.sales_items.data.attributes.name,
                price = item.attributes.unit_price
            )
        } ?: emptyList()
        val receivables = orderResponse.receivables?.map { receivable ->
            val paymentMethod = receivable.paymentMethod?.let {
                PaymentMethod(
                    id = it.id,
                    name = it.name,
                    installments = it.installments ?: 0,
                    interestTax = it.interestTax,
                    paymentType = it.paymentType
                )
            } ?: PaymentMethod(id = 0, name = "", installments = 0, interestTax = 0.0, paymentType = null)
            OrderReceivableItem(
                id = receivable.id,
                documentId = receivable.documentId,
                amountOriginal = receivable.amountOriginal,
                amountFinal = receivable.amountFinal,
                installments = receivable.installments,
                status = parseReceivableStatus(receivable.status),
                paymentMethod = paymentMethod,
                paymentDate = receivable.paymentDate,
                refundDate = null,
                cardLast4 = receivable.cardLast4,
                cardHolder = receivable.cardHolder,
                tax = receivable.tax,
                cardBrand = receivable.cardBrand,
                authorizationId = null,
                authorizationCode = receivable.authorizationCode,
                pixTxIdCode = receivable.pixTxIdCode
            )
        } ?: attributes?.receivables?.data?.map { receivable ->
            val originalAmount = receivable.attributes.amountOriginal
            val paymentMethod = PaymentMethod(
                id = receivable.attributes.payment_methods.data.id,
                name = receivable.attributes.payment_methods.data.attributes.name,
                installments = receivable.attributes.payment_methods.data.attributes.installments,
                interestTax = receivable.attributes.payment_methods.data.attributes.interestTax,
                paymentType = receivable.attributes.payment_methods.data.attributes.paymentType
            )
            val finalAmount = if ((paymentMethod.interestTax ?: 0.0) > 0.0) {
                originalAmount + (originalAmount * (paymentMethod.interestTax ?: 0.0))
            } else {
                receivable.attributes.amountFinal
            }

            OrderReceivableItem(
                id = receivable.id,
                documentId = receivable.documentId,
                amountOriginal = originalAmount,
                amountFinal = finalAmount,
                installments = receivable.attributes.installments,
                status = parseReceivableStatus(receivable.attributes.status),
                paymentMethod = paymentMethod,
                paymentDate = receivable.attributes.paymentDate,
                refundDate = null,
                cardLast4 = receivable.attributes.card_last4,
                cardHolder = receivable.attributes.cardHolder,
                tax = receivable.attributes.tax,
                cardBrand = receivable.attributes.cardBrand,
                authorizationId = null,
                authorizationCode = receivable.attributes.authorizationCode,
                pixTxIdCode = receivable.attributes.pixTxIdCode
            )
        } ?: emptyList()

        return Order(
            id = orderResponse.id,
            customer = OrderCustomer(
                id = customer?.id ?: 0,
                name = customer?.name ?: orderResponse.customerName.orEmpty(),
                cpfCnpj = customer?.cpfCnpj ?: orderResponse.customerCpfCnpj.orEmpty(),
                phoneNumber = customer?.phoneNumber.orEmpty(),
                email = customer?.email
            ),
            serviceName = companyTradeName,
            creationDate = orderResponse.createdAt ?: attributes?.createdAt.orEmpty(),
            status = parseOrderStatus(orderResponse.status ?: attributes?.status),
            vehiclePrice = orderResponse.vehiclePrice ?: attributes?.vehiclePrice ?: 0.0,
            billingDate = orderResponse.billingDate ?: attributes?.billingDate.orEmpty(),
            originalAmount = orderResponse.originalAmount ?: attributes?.originalAmount ?: 0.0,
            currentAmount = orderResponse.currentAmount ?: attributes?.currentAmount ?: 0.0,
            isVehicleFinanced = orderResponse.isVehicleFinanced ?: attributes?.isVehicleFinanced ?: false,
            isVehicleSpecialPlate = orderResponse.isSpecialPlate ?: attributes?.isSpecialPlate ?: false,
            vehicleType = VehicleType(vehicleTypeId, vehicleTypeName),
            items = items,
            receivables = receivables,
            salesman = salesman
        )
    }

    private fun parseOrderStatus(rawStatus: String?): OrderStatus {
        return runCatching {
            OrderStatus.valueOf(rawStatus.orEmpty().uppercase())
        }.getOrDefault(OrderStatus.PENDING)
    }

    private fun parseReceivableStatus(rawStatus: String?): OrderReceivableItemStatus {
        if (rawStatus.equals("reversed", ignoreCase = true)) {
            return OrderReceivableItemStatus.CANCELLED
        }
        return runCatching {
            OrderReceivableItemStatus.valueOf(rawStatus.orEmpty().uppercase())
        }.getOrDefault(OrderReceivableItemStatus.PENDING)
    }

    private fun formatDate(date: String): String {
        return try {
            if (date.contains("-")) {
                // Already in YYYY-MM-DD format
                date.take(10)
            } else {
                // Assume DD/MM/YYYY
                val day = date.substring(0, 2)
                val month = date.substring(3, 5)
                val year = date.substring(6, 10)
                "$year-$month-$day"
            }
        } catch (e: Exception) {
            date
        }
    }

    suspend fun createOrder(
        simulation: Simulation,
        simulationPayments: List<SimulationPayment> = emptyList(),
        salesmanId: Int?
    ): Result<Order> {
        val user = authRepository.getLoggedUser(false)
        val salesCompanyId = user?.companies?.firstOrNull()?.id
        val dispatcherId = user?.dispatchers?.firstOrNull()?.id

        if (salesCompanyId == null || dispatcherId == null) {
            return Result.Error(Exception("ID da empresa ou do despachante não encontrado."))
        }

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

        val simulationRequest = CreateOrderSimulationRequest(
            billingDate = formatDate(simulation.simulation.billingDate),
            vehiclePrice = simulation.simulation.vehiclePrice,
            isVehicleFinanced = simulation.simulation.vehicleDisposal,
            isVehicleSpecialPlate = simulation.simulation.vehicleSpecialPlate,
            vehicleTypeId = simulation.simulation.vehicleTypeId,
            totalPrice = String.format(
                Locale.US,
                "%.2f",
                simulation.simulationItems.sumOf { item ->
                    item.price - (item.discount ?: 0.0)
                }
            )
        )

        val receivablesRequest = simulationPayments.map { simulationPayment ->
            val amountOriginal = Mask.doubleValue(simulationPayment.amountOriginal)
            val tax = simulationPayment.paymentMethod.interestTax ?: 0.0
            val amountFinal = amountOriginal * (1 + tax)

            CreateOrderPaymentRequest(
                paymentMethodId = simulationPayment.paymentMethod.id,
                amountOriginal = amountOriginal.toString(),
                amountFinal = amountFinal.toString(),
                tax = tax,
                installments = simulationPayment.installment,
                paymentDate = formatDate(simulation.simulation.billingDate)
            )
        }

        val itemsRequest = simulation.simulationItems.map { simulationItem ->
            OrderSimulationItemRequest(
                id = simulationItem.id,
                price = formatDecimal(simulationItem.price),
                discount = simulationItem.discount?.let(::formatDecimal)
            )
        }

        val orderRequest = CreateOrderRequest(
            customer = customerRequest,
            salesmanId = salesmanId,
            companyId = salesCompanyId,
            dispatcherId = dispatcherId,
            simulation = simulationRequest,
            receivables = receivablesRequest,
            items = itemsRequest
        )

        Logger.d("Sending CreateOrderRequest: $orderRequest")

        return when (val result = detrapayRemoteDataSource.createOrder(orderRequest)) {
            is Result.Success -> {
                try {
                    val order = parseOrder(result.data.data)
                    invalidateOrdersCache()
                    orderDetailsCache[order.id] = order
                    Result.Success(order)
                } catch (e: Exception) {
                    Logger.d("Error parsing order after creation: ${e.message}")
                    Result.Error(e)
                }
            }
            is Result.Error -> {
                Logger.d("Server returned error in createOrder: ${result.exception.message}")
                result
            }
        }
    }

    suspend fun payOrder(
        orderId:Int,
        receivable: OrderReceivableItem,
        paymentData: PaymentData
    ): Result<Order> {
        val isoDate = formatDate(paymentData.date.orEmpty()) + "T" + paymentData.time.orEmpty()
        when (val result = detrapayRemoteDataSource.payOrderReceivable(receivable, paymentData.copy(date = isoDate))) {
            is Result.Success -> {
                try {
                    invalidateOrdersCache()
                    val parsedOrder = parseOrder(result.data)
                    val order = if (shouldRefetchOrderDetails(result.data, parsedOrder)) {
                        when (val freshOrderResult = getOrder(orderId, forceRefresh = true)) {
                            is Result.Success -> freshOrderResult.data
                            is Result.Error -> parsedOrder
                        }
                    } else {
                        parsedOrder
                    }
                    orderDetailsCache[order.id] = order
                    return Result.Success(order)
                } catch (e: Exception) {
                    Log.e("OrderRepository", "UNABLE TO GET ORDER: ${e.message}")
                    return Result.Error(e)
                }
            }
            is Result.Error -> {
                return Result.Error(result.exception)
            }
            else -> {
                return Result.Error(Exception("Tivemos um erro na atualização do pagamento do pedido com nosso servidor, por favor tente novamente."))
            }
        }
    }

    suspend fun addPendingReceivable(
        orderId: Int,
        paymentMethod: PaymentMethod,
        amountOriginal: Double,
        paymentDate: String? = null
    ): Result<Order> {
        if (amountOriginal <= 0.0) {
            return Result.Error(Exception("Informe um valor maior que zero para adicionar o pagamento."))
        }

        return when (
            val result = detrapayRemoteDataSource.addOrderReceivable(
                orderId = orderId,
                paymentMethodId = paymentMethod.id,
                amountOriginal = amountOriginal,
                installments = paymentMethod.installments.coerceAtLeast(1),
                paymentDate = paymentDate
            )
        ) {
            is Result.Success -> {
                try {
                    val order = parseOrder(result.data)
                    invalidateOrdersCache()
                    orderDetailsCache[order.id] = order
                    Result.Success(order)
                } catch (e: Exception) {
                    Log.e("OrderRepository", "UNABLE TO ADD ORDER RECEIVABLE: ${e.message}")
                    Result.Error(e)
                }
            }
            is Result.Error -> Result.Error(result.exception)
            else -> Result.Error(Exception("Tivemos um erro ao adicionar o pagamento pendente, por favor tente novamente."))
        }
    }

    suspend fun refundOrderPayment(orderId:Int, receivable: OrderReceivableItem, refundDate: RefundPaymentData): Result<Order> {
        val isoDate = formatDate(refundDate.date) + "T" + refundDate.time
        when (val result = detrapayRemoteDataSource.refundOrderReceivableItem(receivable.documentId, isoDate)) {
            is Result.Success -> {
                try {
                    invalidateOrdersCache()
                    val order = parseOrder(result.data)
                    orderDetailsCache[order.id] = order
                    return Result.Success(order)
                } catch (e: Exception) {
                    Log.e("OrderRepository", "UNABLE TO GET ORDER: ${e.message}")
                    return Result.Error(e)
                }
            }
            is Result.Error -> {
                return Result.Error(result.exception)
            }
            else -> {
                return Result.Error(Exception("Tivemos um erro no reembolso do pagamento, por favor tente novamente."))
            }
        }
    }

    suspend fun cancelPendingReceivable(orderId: Int, receivable: OrderReceivableItem): Result<Order> {
        if (!receivable.canBeDeleted()) {
            return Result.Error(Exception("Este pagamento nao pode ser excluido no status atual."))
        }

        when (val result = detrapayRemoteDataSource.deleteOrderReceivableItem(receivable.documentId)) {
            is Result.Success -> {
                return try {
                    invalidateOrdersCache()
                    val order = parseOrder(result.data)
                    orderDetailsCache[order.id] = order
                    Result.Success(order)
                } catch (e: Exception) {
                    Log.e("OrderRepository", "UNABLE TO DELETE ORDER RECEIVABLE: ${e.message}")
                    Result.Error(e)
                }
            }
            is Result.Error -> return Result.Error(result.exception)
            else -> {
                return Result.Error(Exception("Tivemos um erro ao excluir o pagamento, por favor tente novamente."))
            }
        }
    }

    suspend fun updateOrderSalesman(orderId: Int, salesmanId: Int): Result<Order> {
        return when (val result = detrapayRemoteDataSource.updateOrderSalesman(orderId, salesmanId)) {
            is Result.Success -> {
                invalidateOrdersCache()
                runCatching {
                    val order = parseOrder(result.data)
                    orderDetailsCache[order.id] = order
                    Result.Success(order)
                }.getOrElse { Result.Error(it as Exception) }
            }
            is Result.Error -> result
        }
    }

    suspend fun generatePixCharge(receivable: OrderReceivableItem): Result<PixCharge> {
        return detrapayRemoteDataSource.generatePixCharge(
            receivableId = receivable.id,
            amount = receivable.amountFinal,
            paymentDate = receivable.paymentDate
        )
    }

    private fun formatDecimal(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }

    suspend fun updateSplitConfig(receivableId: Int, serial: String): Result<Unit> {
        val splitSerial = if (BuildConfig.DEBUG) {
            DebugConstants.DEBUG_SPLIT_DEVICE_ID
        } else {
            serial
        }
        val splitDescription = if (BuildConfig.DEBUG) DebugConstants.DEBUG_SPLIT_DESCRIPTION else null

        return detrapayRemoteDataSource.updateSplitConfig(
            SplitConfigRequest(
                receivableId = receivableId,
                serial = splitSerial,
                description = splitDescription
            )
        )
    }

    private fun shouldRefetchOrderDetails(orderResponse: OrderResponse, parsedOrder: Order): Boolean {
        val hasReceivablesInResponse = !orderResponse.receivables.isNullOrEmpty() ||
            !orderResponse.attributes?.receivables?.data.isNullOrEmpty()
        val hasAmountsInResponse = (orderResponse.originalAmount != null || orderResponse.attributes?.originalAmount != null) &&
            (orderResponse.currentAmount != null || orderResponse.attributes?.currentAmount != null)

        if (hasReceivablesInResponse && hasAmountsInResponse) {
            return false
        }

        return parsedOrder.receivables.isEmpty() &&
            parsedOrder.originalAmount == 0.0 &&
            parsedOrder.currentAmount == 0.0
    }
}
