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
    val receivables: List<OrderReceivableItem>
) : Serializable

enum class OrderStatus {
    PENDING,
    PAID,
    AUTHORIZED,
    COMPLETED,
    CANCELLED;

    override fun toString(): String {
        return when (this) {
            PENDING -> "Pendente"
            PAID -> "Pago"
            AUTHORIZED -> "Autorizado"
            COMPLETED -> "Concluído"
            CANCELLED -> "Cancelado"
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

data class OrderReceivableItem(
    val id: Int,
    val documentId: String,
    val amountOriginal: Double,
    val amountFinal: Double,
    val tax: Double?,
    val status: OrderReceivableItemStatus,
    val paymentDate: String,
    val cardBrand: String?,
    val cardLast4: String?,
    val cardHolder: String?,
    val authorizationId: String?,
    val authorizationCode: String?,
    val refundDate: String?,
    val pixTxIdCode: String?,
    val installments: Int,
    val paymentMethod: PaymentMethod,
) : Serializable

enum class OrderReceivableItemStatus {
    PENDING,
    PAID,
    CANCELLED,
    REFUNDED;

    override fun toString(): String {
        return when (this) {
            PENDING -> "Pendente"
            PAID -> "Pago"
            CANCELLED -> "Cancelado"
            REFUNDED -> "Estornado"
        }
    }
}