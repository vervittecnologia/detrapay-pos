package com.detrapay.data.datasources.remote

import com.detrapay.data.Result
import com.detrapay.data.api.DetrapayService
import com.detrapay.data.api.SupabaseService
import com.detrapay.data.model.remote.AuthRequest
import com.detrapay.data.model.remote.AuthResponse
import com.detrapay.data.model.remote.CalculateFeesResponse
import com.detrapay.data.model.remote.CreateOrderResponse
import com.detrapay.data.model.remote.CustomerSearchDataResponse
import com.detrapay.data.model.remote.OrderCustomerRequest
import com.detrapay.data.model.remote.OrderReceivableRequest
import com.detrapay.data.model.remote.OrderRequest
import com.detrapay.data.model.remote.OrderResponse
import com.detrapay.data.model.remote.OrderSimulationItemRequest
import com.detrapay.data.model.remote.OrderSimulationRequest
import com.detrapay.data.model.remote.PaymentMethodResponse
import com.detrapay.data.model.remote.SimulationRequest
import com.detrapay.data.model.remote.SimulationResponse
import com.detrapay.data.model.remote.VehicleTypeListResponse
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.remote.ApiError
import com.detrapay.data.model.remote.CardBrandIconResponse
import com.detrapay.data.model.remote.ConfirmPaymentRequest
import com.detrapay.data.model.remote.CreateOrderRequest
import com.detrapay.data.model.remote.RefundOrderReceivableRequest
import com.detrapay.data.model.remote.SplitConfigRequest
import com.detrapay.data.model.remote.UpdateOrderSalesmanRequest
import com.detrapay.data.model.remote.VehicleTypeItemResponse
import com.detrapay.ui.util.Logger
import com.google.gson.Gson
import com.google.gson.JsonElement
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class DetrapayRemoteDataSource @Inject constructor(
    private var detrapayService: DetrapayService,
    private var supabaseService: SupabaseService
) {

    suspend fun calculateFees(
        value: Double,
        paymentType: String,
        brand: String
    ): Result<CalculateFeesResponse> {
        try {
            val result = supabaseService.calculateFees(value, paymentType, brand)
            if (result.isSuccessful) {
                return Result.Success(result.body()!!)
            } else {
                return Result.Error(Exception("Error calculating fees"))
            }
        } catch (e: Throwable) {
            return Result.Error(IOException("Error calculating fees", e))
        }
    }

    suspend fun login(username: String, password: String): Result<AuthResponse> {
        try {
            val authRequest = AuthRequest(identifier = username, password = password)
            val result = detrapayService.auth(authRequest)
            Logger.d(result.toString())
            if (result.isSuccessful) {
                Logger.d((result.body() ?: "").toString())
                return Result.Success(result.body()!!)
            } else {
                Logger.d((result.errorBody() ?: "").toString())
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                return Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            Logger.d(e.toString())
            return Result.Error(IOException("Error Loggerging in", e))
        }
    }

    suspend fun getCompanies(): Result<Any> {
        try {
            val result = detrapayService.getCompanies()
            if (result.isSuccessful) {
                Logger.d((result.body() ?: "").toString())
                return Result.Success(result.body()!!)
            } else {
                Logger.d((result.errorBody() ?: "").toString())
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                return Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            Logger.d(e.toString())
            return Result.Error(IOException("Error getting employees", e))
        }
    }

    suspend fun getOrders(companyId: Int, dispatcherId: Int): Result<List<OrderResponse>> {
        try {
            val result = detrapayService.getOrders(companyId, dispatcherId)
            if (result.isSuccessful) {
                Logger.d((result.body() ?: "").toString())
                return Result.Success(result.body()!!.data)
            } else {
                Logger.d((result.errorBody() ?: "").toString())
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                return Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            Logger.d(e.toString())
            return Result.Error(IOException("Error getting stores", e))
        }
    }

    suspend fun getOrder(orderId: Int): Result<OrderResponse> {
        try {
            val result = detrapayService.getOrder(orderId)
            if (result.isSuccessful) {
                Logger.d((result.body() ?: "").toString())
                return Result.Success(result.body()!!.data)
            } else {
                Logger.d((result.errorBody() ?: "").toString())
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                return Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            Logger.d(e.toString())
            return Result.Error(IOException("Error getting stores", e))
        }
    }

    suspend fun getVehicleTypes(): Result<List<VehicleTypeItemResponse>> {
        try {
            val result = detrapayService.getVehicleTypes()
            if (result.isSuccessful) {
                Logger.d((result.body() ?: "").toString())
                return Result.Success(result.body()!!.data)
            } else {
                Logger.d((result.errorBody() ?: "").toString())
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                return Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            Logger.d(e.toString())
            return Result.Error(IOException("Error getting vehicleTypes", e))
        }
    }

    suspend fun getPaymentMethods(): Result<List<PaymentMethodResponse>> {
        try {
            val result = detrapayService.getPaymentMethods()
            if (result.isSuccessful) {
                Logger.d((result.body() ?: "").toString())
                return Result.Success(result.body()!!)
            } else {
                Logger.d((result.errorBody() ?: "").toString())
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                return Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            Logger.d(e.toString())
            return Result.Error(IOException("Error getting payment methods", e))
        }
    }

    suspend fun searchCustomer(cpfCnpj: String): Result<CustomerSearchDataResponse> {
        try {
            val result = detrapayService.searchCustomer(cpfCnpj)
            if (result.isSuccessful) {
                Logger.d((result.body() ?: "").toString())
                return Result.Success(result.body()!!)
            } else {
                Logger.d((result.errorBody() ?: "").toString())
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                return Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            Logger.d(e.toString())
            return Result.Error(IOException("Error getting customer data", e))
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
        specialPlate: Boolean,
        companyId: Int,
        dispatcherId: Int
    ): Result<SimulationResponse> {
        try {
            val simulationRequest = SimulationRequest(
                cpf_cnpj = cpfCnpj,
                name = clientName,
                phone_number = whatsapp,
                billing_date = invoiceDate,
                vehicle_price = vehicleValue,
                vehicle_type_id = vehicleTypeId,
                is_vehicle_financed = disposalVehicle,
                is_vehicle_special_plate = specialPlate,
                company_id = companyId,
                dispatcher_id = dispatcherId

            )
            val result = detrapayService.simulate(simulationRequest)
            if (result.isSuccessful) {
                Logger.d((result.body() ?: "").toString())
                return Result.Success(result.body()!!)
            } else {
                Logger.d((result.errorBody() ?: "").toString())
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                return Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            Logger.d(e.toString())
            return Result.Error(IOException("Error simulating", e))
        }
    }

    suspend fun createOrder(
        orderRequest: CreateOrderRequest
    ): Result<CreateOrderResponse> {
        try {
            val result = detrapayService.createOrder(orderRequest)
            if (result.isSuccessful) {
                Logger.d((result.body() ?: "").toString())
                return Result.Success(result.body()!!)
            } else {
                Logger.d((result.errorBody() ?: "").toString())
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                return Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            Logger.d(e.toString())
            return Result.Error(IOException("Error creating order", e))
        }
    }

    suspend fun updateOrder(
        orderId: Int,
        customer: OrderCustomerRequest,
        simulation: OrderSimulationRequest,
        items: List<OrderSimulationItemRequest>,
        receivables: List<OrderReceivableRequest>,
        createdById: String? = null
    ): Result<OrderResponse> {
        try {
            val orderRequest = OrderRequest(
                customer = customer,
                simulation = simulation,
                items = items,
                receivables = receivables,
                createdById = createdById
            )
            val result = detrapayService.updateOrder(orderId, orderRequest)
            if (result.isSuccessful) {
                Logger.d((result.body() ?: "").toString())
                return Result.Success(result.body()!!.data)
            } else {
                Logger.d((result.errorBody() ?: "").toString())
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                return Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            Logger.d(e.toString())
            return Result.Error(IOException("Error creating order", e))
        }
    }

    suspend fun payOrderReceivable(
        receivable: OrderReceivableItem,
        paymentData: PaymentData
    ): Result<Unit> {
        try {
            val transactionLogJson: JsonElement? = try {
                paymentData.transactionLog?.let { Gson().fromJson(it, JsonElement::class.java) }
            } catch (e: Exception) {
                null
            }

            val confirmPaymentRequest = ConfirmPaymentRequest(
                authorizationCode = paymentData.transactionCode,
                paymentDate = paymentData.date,
                cardBrand = paymentData.cardBrand ?: "",
                cardHolder = paymentData.cardHolder ?: "",
                cardLast4 = paymentData.cardLast4 ?: "",
                transactionLog = transactionLogJson
            )
            val result = detrapayService.confirmPayment(receivable.id.toString(), confirmPaymentRequest)
            if (result.isSuccessful) {
                return Result.Success(Unit)
            } else {
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                return Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            Logger.d(e.toString())
            return Result.Error(IOException("Erro no pagamento do pedido", e))
        }
    }

    suspend fun refundOrderReceivableItem(
        receivableId: String,
        refundDate: String
    ): Result<Unit> {
        try {
            val updateReceivableItemRequest = RefundOrderReceivableRequest(
                refundDate = refundDate,
                status =  OrderReceivableItemStatus.REFUNDED.name.lowercase(),
            )
            val result = detrapayService.refundOrderReceivableItem(receivableId, updateReceivableItemRequest)
            if (result.isSuccessful) {
                return Result.Success(Unit)
            } else {
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                return Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            Logger.d(e.toString())
            return Result.Error(IOException("Erro no reembolso do pagamento", e))
        }
    }

    suspend fun updateOrderSalesman(orderId: Int, salesmanId: String): Result<OrderResponse> {
        try {
            val request = UpdateOrderSalesmanRequest(salesmanId)
            val result = detrapayService.updateOrderSalesman(orderId, request)

            return if (result.isSuccessful) {
                Result.Success(result.body()!!)
            } else {
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            return Result.Error(IOException("Erro ao atualizar vendedor do pedido", e))
        }
    }
    
    suspend fun updateSplitConfig(payload: SplitConfigRequest): Result<Unit> {
        try {
            val result = detrapayService.updateSplitConfig(payload)
            if (result.isSuccessful) {
                Logger.d("updateSplitConfig success")
                return Result.Success(Unit)
            } else {
                Logger.d((result.errorBody() ?: "").toString())
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                return Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            Logger.d(e.toString())
            return Result.Error(IOException("Error in update-split-config", e))
        }
    }

    suspend fun getCardBrandIcons(brands: String): Result<List<CardBrandIconResponse>> {
        try {
            val result = supabaseService.getCardBrandIcons(brands)
            if (result.isSuccessful) {
                return Result.Success(result.body()!!)
            } else {
                if (result.code() == 401) return Result.Error(UnauthorizedException())
                return Result.Error(Exception(ApiError(result.errorBody()).message))
            }
        } catch (e: Throwable) {
            return Result.Error(IOException("Error getting card brand icons", e))
        }
    }

    suspend fun downloadFile(url: String): Result<ResponseBody> {
        try {
            val result = detrapayService.downloadFile(url)
            if (result.isSuccessful) {
                return Result.Success(result.body()!!)
            } else {
                return Result.Error(Exception("Error downloading file"))
            }
        } catch (e: Throwable) {
            return Result.Error(IOException("Error downloading file", e))
        }
    }
}
