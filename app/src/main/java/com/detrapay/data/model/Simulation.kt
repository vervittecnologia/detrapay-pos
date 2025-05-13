package com.detrapay.data.model

data class Simulation(
    val customer: SimulationCustomer,
    val simulation: SimulationSimulation,
    val simulationItems: List<SimulationItem>
)

data class SimulationCustomer(
    val cpfCnpj: String,
    val name: String,
    val whatsapp: String,
)

data class SimulationSimulation(
    val billingDate: String,
    val vehiclePrice: String,
    val vehicleDisposal: Boolean,
    val vehicleSpecialPlate: Boolean,
    val totalPrice: Double,
    val vehicleTypeId: Int,
)

data class SimulationItem(
    val id: Int,
    val name: String,
    val discountAllowed: Boolean,
    val price: Double,
    val discount: Double?,
)

data class SimulationPayment(
    var id: Long,
    val paymentMethod: PaymentMethod,
    val amount: String,
    val installment: Int = 1
)