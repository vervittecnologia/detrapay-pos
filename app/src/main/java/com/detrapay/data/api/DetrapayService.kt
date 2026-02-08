package com.detrapay.data.api

import com.detrapay.data.model.remote.AuthRequest
import com.detrapay.data.model.remote.AuthResponse
import com.detrapay.data.model.remote.CompanyListResponse
import com.detrapay.data.model.remote.CustomerSearchDataResponse
import com.detrapay.data.model.remote.OrderRequest
import com.detrapay.data.model.remote.OrderResponse
import com.detrapay.data.model.remote.PaginatedOrderResponse
import com.detrapay.data.model.remote.PaymentMethodResponse
import com.detrapay.data.model.remote.RefundOrderReceivableRequestDataWrapper
import com.detrapay.data.model.remote.SimulationRequest
import com.detrapay.data.model.remote.SimulationResponse
import com.detrapay.data.model.remote.UpdateOrderReceivableRequestDataWrapper
import com.detrapay.data.model.remote.VehicleTypeListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface DetrapayService {

    @POST("auth/local")
    suspend fun auth(@Body payload: AuthRequest): Response<AuthResponse>

    @GET("companies")
    suspend fun getCompanies(): Response<CompanyListResponse>

    @GET("orders?populate=deep,3")
    suspend fun getOrders(): Response<PaginatedOrderResponse>

    @GET("orders/{id}?populate=deep,3")
    suspend fun getOrder(@Path("id") orderId: Int): Response<OrderResponse>

    @GET("vehicle-types/me")
    suspend fun getVehicleTypes(): Response<VehicleTypeListResponse>

    @GET("payment-methods")
    suspend fun getPaymentMethods(): Response<List<PaymentMethodResponse>>

    @GET("customers/{cpfCnpj}")
    suspend fun searchCustomer(@Path("cpfCnpj") cpfCnpj: String): Response<CustomerSearchDataResponse>

    @POST("mobile/simulate")
    suspend fun simulate(@Body payload: SimulationRequest): Response<SimulationResponse>

    @POST("mobile/orders")
    suspend fun createOrder(@Body payload: OrderRequest): Response<OrderResponse>

    @PUT("mobile/orders/{id}")
    suspend fun updateOrder(@Path("id") id: Int, @Body payload: OrderRequest): Response<OrderResponse>

    @PUT("order-receivables/{id}")
    suspend fun updateOrderReceivableItem(@Path("id") id: String, @Body payload: UpdateOrderReceivableRequestDataWrapper): Response<OrderResponse>

    @PUT("order-receivables/{id}")
    suspend fun refundOrderReceivableItem(@Path("id") id: String, @Body payload: RefundOrderReceivableRequestDataWrapper): Response<OrderResponse>

}
