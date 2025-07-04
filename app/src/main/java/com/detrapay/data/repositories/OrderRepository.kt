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
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.VehicleType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val detrapayRemoteDataSource: DetrapayRemoteDataSource
) {

    suspend fun getOrders(): Result<List<Order>> {
        when (val result = detrapayRemoteDataSource.getOrders()) {
            is Result.Success -> {
                try {
                    val orders: List<Order?> = result.data.map {
                        try {
                            Order(
                                id = it.id,
                                customer = OrderCustomer(
                                    id = it.customer.id,
                                    name = it.customer.name,
                                    cpfCnpj = it.customer.cpfCnpj,
                                    phoneNumber = it.customer.phoneNumber,
                                    email = it.customer.email
                                ),
                                serviceName = "Primeiro emplacamento", // TODO RECEIVE NAME
                                creationDate = it.createdAt,
                                status = OrderStatus.PENDING, // TODO CREATE PARSER
                                vehiclePrice = it.vehiclePrice,
                                billingDate = it.billingDate,
                                originalAmount = it.originalAmount,
                                currentAmount = it.currentAmount,
                                isVehicleFinanced = it.isVehicleFinanced,
                                isVehicleSpecialPlate = it.isVehicleSpecialPlate,
                                vehicleType = VehicleType(it.vehicleType?.id ?: 0, it.vehicleType?.name ?: ""),
                                items = it.items.map { item ->
                                    OrderItem(
                                        id = item.id,
                                        totalPrice = item.totalPrice,
                                        discount = item.discount,
                                        salesItemId = item.salesItem?.id,
                                        name = item.salesItem?.name,
                                        price = item.salesItem?.price,
                                    )
                                }.toList(),
                                receivables = it.receivables.map { receivable ->
                                    OrderReceivableItem(
                                        id = receivable.id,
                                        amountOriginal = receivable.amountOriginal,
                                        amountFinal = receivable.amountFinal,
                                        tax = receivable.tax,
                                        status = OrderReceivableItemStatus.PENDING, // TODO CREATE PARSER
                                        paymentDate = receivable.paymentDate,
                                        cardBrand = receivable.cardBrand,
                                        cardLast4 = receivable.cardLast4,
                                        authorizationCode = receivable.authorizationCode,
                                        installments = receivable.installments,
                                        paymentMethod = PaymentMethod(
                                            id = receivable.paymentMethod.id,
                                            name = receivable.paymentMethod.name,
                                            maxInstallments = receivable.paymentMethod.maxInstallments,
                                            interestRate = receivable.paymentMethod.interestRate
                                        ),
                                    )

                                }
                            )
                        } catch (e:Exception){
                            null
                        }
                    }
                    return Result.Success(orders.filterNotNull())
                } catch (e: Exception) {
                    Log.d("UNABLE TO GET ORDERS", e.message ?: "")
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
                    val orders: Order = result.data.let {
                        Order(
                            id = it.id,
                            customer = OrderCustomer(
                                id = it.customer.id,
                                name = it.customer.name,
                                cpfCnpj = it.customer.cpfCnpj,
                                phoneNumber = it.customer.phoneNumber,
                                email = it.customer.email
                            ),
                            serviceName = "Primeiro emplacamento", // TODO RECEIVE NAME
                            creationDate = it.createdAt,
                            status = OrderStatus.PENDING, // TODO CREATE PARSER
                            vehiclePrice = it.vehiclePrice,
                            billingDate = it.billingDate,
                            originalAmount = it.originalAmount,
                            currentAmount = it.currentAmount,
                            isVehicleFinanced = it.isVehicleFinanced,
                            isVehicleSpecialPlate = it.isVehicleSpecialPlate,
                            vehicleType = VehicleType(it.vehicleType?.id ?: 0, it.vehicleType?.name ?: ""),
                            items = it.items.map { item ->
                                OrderItem(
                                    id = item.id,
                                    totalPrice = item.totalPrice,
                                    discount = item.discount,
                                    salesItemId = item.salesItem?.id,
                                    name = item.salesItem?.name,
                                    price = item.salesItem?.price,
                                )
                            }.toList(),
                            receivables = it.receivables.map { receivable ->
                                OrderReceivableItem(
                                    id = receivable.id,
                                    amountFinal = receivable.amountFinal,
                                    amountOriginal = receivable.amountOriginal,
                                    tax = receivable.tax,
                                    status = OrderReceivableItemStatus.PENDING, // TODO CREATE PARSER
                                    paymentDate = receivable.paymentDate,
                                    cardBrand = receivable.cardBrand,
                                    cardLast4 = receivable.cardLast4,
                                    authorizationCode = receivable.authorizationCode,
                                    installments = receivable.installments,
                                    paymentMethod = PaymentMethod(
                                        id = receivable.paymentMethod.id,
                                        name = receivable.paymentMethod.name,
                                        maxInstallments = receivable.paymentMethod.maxInstallments,
                                        interestRate = receivable.paymentMethod.interestRate
                                    ),
                                )

                            }
                        )
                    }
                    return Result.Success(orders)
                } catch (e: Exception) {
                    Log.d("UNABLE TO GET ORDER", e.message ?: "")
                    return Result.Error(e)
                }
            }
            is Result.Error -> {
                return result
            }else -> {
                return Result.Error(Exception())
            }
        }
    }
}