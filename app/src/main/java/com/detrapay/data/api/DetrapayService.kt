package com.detrapay.data.api

import com.detrapay.data.model.remote.AuthRequest
import com.detrapay.data.model.remote.AuthResponse
import com.detrapay.data.model.remote.CustomerSearchDataResponse
import com.detrapay.data.model.remote.EmployeeResponse
import com.detrapay.data.model.remote.OrderRequest
import com.detrapay.data.model.remote.OrderResponse
import com.detrapay.data.model.remote.PaymentMethodResponse
import com.detrapay.data.model.remote.SimulationRequest
import com.detrapay.data.model.remote.SimulationResponse
import com.detrapay.data.model.remote.VehicleTypeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface DetrapayService {
    @POST("auth/local")
    suspend fun auth(@Body auth: AuthRequest): Response<AuthResponse>

    @GET("company-users/users")
    suspend fun getEmployees(): Response<EmployeeResponse>

    @GET("sales-orders/me")
    suspend fun getOrders(): Response<List<OrderResponse>>

    @GET("sales-orders/{id}/me")
    suspend fun getOrder(@Path("id") orderId: Int): Response<OrderResponse>

    @POST("sales-orders/me")
    suspend fun createOrder(@Body orderRequest: OrderRequest): Response<OrderResponse>

    @GET("payment-methods/me")
    suspend fun getPaymentMethods(): Response<List<PaymentMethodResponse>>

    @GET("vehicle-types/me")
    suspend fun getVehicleTypes(): Response<VehicleTypeResponse>

    @GET("customers/{cpfcnpj}/me")
    suspend fun searchCustomer(@Path("cpfcnpj") cpfCnpj: String): Response<CustomerSearchDataResponse>

    @POST("sales-items/simulation/me")
    suspend fun simulate(@Body simulation: SimulationRequest): Response<SimulationResponse>
}