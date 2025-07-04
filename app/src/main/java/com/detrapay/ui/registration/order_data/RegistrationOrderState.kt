package com.detrapay.ui.registration.order_data

import com.detrapay.data.model.VehicleType

data class RegistrationOrderInitialState(
    val vehicleTypes: List<VehicleType>,
    val orderData: OrderData? = null,
)

data class OrderData(
    val cpfCnpj: String,
    val phone: String,
    val name: String,
    val invoiceDate: String,
    val vehiclePrice: String,
    val specialPlate: Boolean,
    val disposalVehicle: Boolean,
    val vehicleType: VehicleType
)

class RegistrationOrderState()