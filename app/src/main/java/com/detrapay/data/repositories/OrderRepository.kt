package com.detrapay.data.repositories

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
import com.detrapay.data.model.VehicleType
import com.detrapay.data.model.remote.OrderResponse
import com.detrapay.ui.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val detrapayRemoteDataSource: DetrapayRemoteDataSource
) {

    suspend fun getOrders(forceRefresh: Boolean = false): Result<List<Order>> {
        when (val result = detrapayRemoteDataSource.getOrders()) {
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
                    Logger.d("UNABLE TO GET ORDERS: ${e.message}")
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
                    Logger.d("UNABLE TO GET ORDER: ${e.message}")
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
                id = orderResponse.customer.id,
                name = orderResponse.customer.name,
                cpfCnpj = orderResponse.customer.cpfCnpj,
                phoneNumber = orderResponse.customer.phoneNumber,
                email = orderResponse.customer.email
            ),
            serviceName = "Primeiro emplacamento",
            creationDate = orderResponse.createdAt,
            status = OrderStatus.valueOf(orderResponse.status.name),
            vehiclePrice = orderResponse.vehiclePrice,
            billingDate = orderResponse.billingDate,
            originalAmount = orderResponse.originalAmount,
            currentAmount = orderResponse.currentAmount,
            isVehicleFinanced = orderResponse.isVehicleFinanced,
            isVehicleSpecialPlate = orderResponse.isVehicleSpecialPlate,
            vehicleType = VehicleType(
                orderResponse.vehicleType?.id ?: 0,
                orderResponse.vehicleType?.name ?: ""
            ),
            items = orderResponse.items.map { item ->
                OrderItem(
                    id = item.id,
                    totalPrice = item.totalPrice,
                    discount = item.discount,
                    salesItemId = item.salesItem?.id,
                    name = item.salesItem?.name,
                    price = item.salesItem?.price,
                )
            }.toList(),
            receivables = orderResponse.receivables.map { receivable ->
                OrderReceivableItem(
                    id = receivable.id,
                    documentId = receivable.documentId,
//                    amountFinal = if ( receivable.tax != null ) {
//                        receivable.amountFinal * receivable.tax
//                    } else {
//                        receivable.amountFinal
//                    },
                    amountFinal = receivable.amountFinal,
                    amountOriginal = receivable.amountOriginal,
                    tax = receivable.tax,
                    status = OrderReceivableItemStatus.valueOf(receivable.status.name),
                    paymentDate = receivable.paymentDate,
                    cardBrand = receivable.cardBrand,
                    cardLast4 = receivable.cardLast4,
                    cardHolder = receivable.cardHolder,
                    refundDate = receivable.refundDate,
                    authorizationCode = receivable.authorizationCode,
                    authorizationId = receivable.authorizationId,
                    installments = receivable.installments,
                    pixTxIdCode = receivable.pixTxIdCode,
                    paymentMethod = PaymentMethod(
                        id = receivable.paymentMethod.id,
                        name = receivable.paymentMethod.name,
                        maxInstallments = receivable.paymentMethod.maxInstallments,
                        interestRate = receivable.paymentMethod.interestRate,
                    )
                )
            }.toList()
        )
    }

    private fun formatDate(date: String): String {
        val day = date.substring(0, 2)
        val month = date.substring(3, 5)
        val year = date.substring(6, 10)
        return "$year-$month-$day"
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
                    Logger.d("UNABLE TO GET ORDER: ${e.message}")
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
                    Logger.d("UNABLE TO GET ORDER: ${e.message}")
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
}