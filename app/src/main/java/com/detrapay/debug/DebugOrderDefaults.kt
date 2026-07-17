package com.detrapay.debug

import com.detrapay.data.model.Salesman
import com.detrapay.data.model.VehicleType
import com.detrapay.ui.registration.order_data.OrderData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugOrderDefaults {
    private const val LOGIN_CNPJ = "04685620000162"
    private const val LOGIN_PASSWORD = "crasa04685620"
    private const val CPF = "05257121352"
    private const val CLIENT_NAME = "Joao Vitor Lima"
    private const val WHATSAPP = "11987654321"
    private const val VEHICLE_PRICE = "89.900,00"
    private const val SPECIAL_PLATE = false
    private const val DISPOSAL_VEHICLE = true

    fun loginCnpj(): String = LOGIN_CNPJ

    fun loginPassword(): String = LOGIN_PASSWORD

    fun loginCnpjMasked(): String {
        return "${LOGIN_CNPJ.substring(0, 2)}.${LOGIN_CNPJ.substring(2, 5)}." +
            "${LOGIN_CNPJ.substring(5, 8)}/${LOGIN_CNPJ.substring(8, 12)}-" +
            LOGIN_CNPJ.substring(12, 14)
    }

    fun createOrderData(
        vehicleTypes: List<VehicleType>,
        salesmen: List<Salesman>
    ): OrderData? {
        val vehicleType = vehicleTypes.firstOrNull() ?: return null

        return OrderData(
            cpfCnpj = CPF,
            phone = WHATSAPP,
            name = CLIENT_NAME,
            invoiceDate = currentInvoiceDate(),
            vehiclePrice = VEHICLE_PRICE,
            specialPlate = SPECIAL_PLATE,
            disposalVehicle = DISPOSAL_VEHICLE,
            vehicleType = vehicleType,
            salesmanId = salesmen.firstOrNull()?.id
        )
    }

    private fun currentInvoiceDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
