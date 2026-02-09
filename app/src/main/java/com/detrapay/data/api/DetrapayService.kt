package com.detrapay.data.api

import com.detrapay.data.model.remote.AuthRequest
import com.detrapay.data.model.remote.AuthResponse
import com.detrapay.data.model.remote.CompanyListResponse
import com.detrapay.data.model.remote.CreateOrderResponse
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
import retrofit2.http.Query

interface DetrapayService {

    @POST("auth/local")
    suspend fun auth(@Body payload: AuthRequest): Response<AuthResponse>

    @GET("companies")
    suspend fun getCompanies(): Response<CompanyListResponse>

    @GET("orders")
    suspend fun getOrders(@Query("populate") populate: String = "deep,3"): Response<PaginatedOrderResponse>

    @GET("sales-orders/{id}")
    suspend fun getOrder(@Path("id") orderId: Int, @Query("populate") populate: String = "deep,3"): Response<CreateOrderResponse>

    @GET("vehicle-types")
    suspend fun getVehicleTypes(): Response<VehicleTypeListResponse>

    @GET("payment-methods")
    suspend fun getPaymentMethods(): Response<List<PaymentMethodResponse>>

    @GET("customers/{cpfCnpj}")
    suspend fun searchCustomer(@Path("cpfCnpj") cpfCnpj: String): Response<CustomerSearchDataResponse>

    @POST("sales-items/simulation")
    suspend fun simulate(@Body payload: SimulationRequest): Response<SimulationResponse>

    @POST("orders")
    suspend fun createOrder(@Body payload: OrderRequest): Response<CreateOrderResponse>

    @POST("orders/{id}")
    suspend fun updateOrder(@Path("id") id: Int, @Body payload: OrderRequest): Response<CreateOrderResponse>

    @PUT("order-receivables/{id}")
    suspend fun updateOrderReceivableItem(@Path("id") id: String, @Body payload: UpdateOrderReceivableRequestDataWrapper): Response<OrderResponse>

    @PUT("order-receivables/{id}")
    suspend fun refundOrderReceivableItem(@Path("id") id: String, @Body payload: RefundOrderReceivableRequestDataWrapper): Response<OrderResponse>

}
