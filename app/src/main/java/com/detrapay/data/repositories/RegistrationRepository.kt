package com.detrapay.data.repositories

import android.util.Log
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
import com.detrapay.data.model.remote.OrderSimulationItemRequest
import com.detrapay.data.model.remote.OrderSimulationRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegistrationRepository @Inject constructor(
    private val detrapayRemoteDataSource: DetrapayRemoteDataSource
) {

    suspend fun loadVehicleTypes(): Result<List<VehicleType>> {
        when (val result = detrapayRemoteDataSource.getVehicleTypes()) {
            is Result.Success -> {
                try {
                    val vehicleTypes = result.data.map {
                        VehicleType(it.id, it.name)
                    }
                    Log.d("UEHARINHA", vehicleTypes.toString())
                    return Result.Success(vehicleTypes)
                } catch (e: Exception) {
                    Log.d("UNABLE TO LOAD VEHICLE TYPES", e.message ?: "")
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
                    Log.d("UEHARINHA", paymentMethods.toString())
                    return Result.Success(paymentMethods)
                } catch (e: Exception) {
                    Log.d("UNABLE TO LOAD PAYMENT METHODS", e.message ?: "")
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
                        Log.d("UEHARINHA", customerSearchData.toString())
                        return Result.Success(customerSearchData)
                    }
                } catch (e: Exception) {
                    Log.d("UNABLE TO LOAD PAYMENT METHODS", e.message ?: "")
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
                        Log.d("UEHARINHA", result.data.toString())
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
                    Log.d("UNABLE TO SIMULATE", e.message ?: "")
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
        simulationPayments: List<SimulationPayment>
    ): Result<Order> {
        val customerRequest = OrderCustomerRequest(
            name = simulation.customer.name,
            cpfCnpj = simulation.customer.cpfCnpj.replace(".", "")
                .replace("/", "")
                .replace("-", ""),
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
                amount = simulationPayment.amount
                    .replace("R$", "")
                    .replace(" ", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .replace("\\s".toRegex(), ""),
                paymentDate = "",
                installments = simulationPayment.installment
            )
        }

        when (val result = detrapayRemoteDataSource.createOrder(
            customer = customerRequest,
            simulation = simulationRequest,
            simulationItems = simulationItemsRequest,
            receivables = receivablesRequest
        )) {
            is Result.Success -> {
                try {
                    result.data.let {
                        val order = Order(
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
                            vehicleType = VehicleType(
                                it.vehicleType?.id ?: 0,
                                it.vehicleType?.name ?: ""
                            ),
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
                                    amount = receivable.amount,
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
                        return Result.Success(order)
                    }
                } catch (e: Exception) {
                    Log.d("UNABLE TO CREATE ORDER", e.message ?: "")
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
