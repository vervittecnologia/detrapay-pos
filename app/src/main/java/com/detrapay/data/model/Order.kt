package com.detrapay.data.model

import java.io.Serializable

data class Order(
    val id: Int,
    val serviceName: String,
    val status: OrderStatus,
    val creationDate: String,
    val vehiclePrice: Double,
    val billingDate: String,
    val originalAmount: Double,
    val currentAmount: Double,
    val isVehicleFinanced: Boolean,
    val isVehicleSpecialPlate: Boolean,
    val customer: OrderCustomer,
    val vehicleType: VehicleType,
    val items: List<OrderItem>,
    val receivables: List<OrderReceivableItem>,
    val salesman: Salesman?,
    val isMemoryOnlyPayment: Boolean = false
) : Serializable

enum class OrderStatus {
    PENDING,
    IN_PROGRESS,
    PAID,
    AUTHORIZED,
    COMPLETED,
    CANCELLED;

    override fun toString(): String {
        return when (this) {
            PENDING -> "Pendente"
            IN_PROGRESS, PAID, AUTHORIZED -> "Em Progresso"
            COMPLETED -> "Concluído"
            CANCELLED -> "Cancelado"
        }
    }

    companion object {
        fun fromApi(status: String?): OrderStatus = when (status?.trim()?.lowercase()) {
            "quote", "pending", null, "" -> PENDING
            "in_progress", "awaiting_review", "authorized", "paid" -> IN_PROGRESS
            "completed" -> COMPLETED
            "reversed", "denied", "cancelled" -> CANCELLED
            else -> PENDING
        }
    }
}

data class OrderCustomer(
    val id: Int,
    val name: String,
    val cpfCnpj: String,
    val phoneNumber: String,
    val email: String?
) : Serializable

data class OrderItem(
    val id: Int,
    val totalPrice: Double,
    val discount: Double,
    val salesItemId: Int?,
    val name: String?,
    val price: Double?
) : Serializable
