package com.detrapay.data.api

import com.detrapay.data.model.remote.AuthRequest
import com.detrapay.data.model.remote.AuthResponse
import com.detrapay.data.model.remote.AddOrderReceivableRequest
import com.detrapay.data.model.remote.AtomicPaymentMutationResponse
import com.detrapay.data.model.remote.CardBrandIconResponse
import com.detrapay.data.model.remote.CompanyListResponse
import com.detrapay.data.model.remote.ConfirmPaymentRequest
import com.detrapay.data.model.remote.CompleteOnlinePaymentRequest
import com.detrapay.data.model.remote.CreateOrderRequest
import com.detrapay.data.model.remote.CreateOrderResponse
import com.detrapay.data.model.remote.CustomerSearchDataResponse
import com.detrapay.data.model.remote.OrderRequest
import com.detrapay.data.model.remote.OrderDocumentListResponse
import com.detrapay.data.model.remote.OrderDocumentMutationResponse
import com.detrapay.data.model.remote.OrderResponse
import com.detrapay.data.model.remote.PaginatedOrderResponse
import com.detrapay.data.model.remote.PaymentMethodResponse
import com.detrapay.data.model.remote.PaymentAttemptResponse
import com.detrapay.data.model.remote.PrepareOnlinePaymentRequest
import com.detrapay.data.model.remote.PixChargeRequest
import com.detrapay.data.model.remote.PixChargeResponse
import com.detrapay.data.model.remote.RefundOrderReceivableRequest
import com.detrapay.data.model.remote.RecordManualPaymentRequest
import com.detrapay.data.model.remote.RefreshSessionRequest
import com.detrapay.data.model.remote.OrderReceivableMutationResponse
import com.detrapay.data.model.remote.SalespeopleResponse
import com.detrapay.data.model.remote.SessionRefreshResponse
import com.detrapay.data.model.remote.SimulationRequest
import com.detrapay.data.model.remote.SimulationResponse
import com.detrapay.data.model.remote.SplitConfigRequest
import com.detrapay.data.model.remote.UpdateOrderReceivableRequest
import com.detrapay.data.model.remote.UpdateOrderSalesmanRequest
import com.detrapay.data.model.remote.VehicleTypeListResponse
import retrofit2.http.Url
import okhttp3.ResponseBody
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface DetrapayService {

    @POST("auth/local")
    suspend fun auth(@Body payload: AuthRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refresh(
        @Header("x-refresh-token") refreshTokenHeader: String,
        @Body payload: RefreshSessionRequest,
    ): Response<SessionRefreshResponse>

    @GET("companies")
    suspend fun getCompanies(): Response<CompanyListResponse>

    @GET("orders/summary")
    suspend fun getOrders(
        @Query("companyId") companyId: Int,
        @Query("dispatcherId") dispatcherId: Int,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<PaginatedOrderResponse>

    @GET("sales-orders/{id}")
    suspend fun getOrder(@Path("id") orderId: Int): Response<CreateOrderResponse>

    @PUT("sales-orders/{id}")
    suspend fun updateOrderSalesman(@Path("id") orderId: Int, @Body payload: UpdateOrderSalesmanRequest): Response<CreateOrderResponse>

    @GET("vehicle-types")
    suspend fun getVehicleTypes(): Response<VehicleTypeListResponse>

    @GET("salespeople")
    suspend fun getSalespeople(
        @Query("company_id") companyId: Int,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 100
    ): Response<SalespeopleResponse>

    @GET("payment-methods")
    suspend fun getPaymentMethods(): Response<List<PaymentMethodResponse>>

    @GET("customers/{cpfCnpj}")
    suspend fun searchCustomer(@Path("cpfCnpj") cpfCnpj: String): Response<CustomerSearchDataResponse>

    @POST("sales-items/simulation")
    suspend fun simulate(@Body payload: SimulationRequest): Response<SimulationResponse>

    @POST("orders")
    suspend fun createOrder(@Body payload: CreateOrderRequest): Response<CreateOrderResponse>

    @POST("orders/{id}")
    suspend fun updateOrder(@Path("id") id: Int, @Body payload: OrderRequest): Response<CreateOrderResponse>

    @GET("orders/{id}/documents")
    suspend fun getOrderDocuments(
        @Path("id") id: Int,
    ): Response<OrderDocumentListResponse>

    @Multipart
    @POST("orders/{id}/documents")
    suspend fun uploadOrderDocument(
        @Path("id") id: Int,
        @Part file: MultipartBody.Part,
    ): Response<OrderDocumentMutationResponse>

    @POST("orders/{id}/receivables")
    suspend fun addOrderReceivable(
        @Path("id") id: Int,
        @Body payload: AddOrderReceivableRequest
    ): Response<OrderReceivableMutationResponse>

    @POST("orders/{id}/payment-attempts")
    suspend fun prepareOnlinePayment(
        @Path("id") id: Int,
        @Body payload: PrepareOnlinePaymentRequest,
    ): Response<PaymentAttemptResponse>

    @POST("payment-attempts/{id}/complete")
    suspend fun completeOnlinePayment(
        @Path("id") id: String,
        @Body payload: CompleteOnlinePaymentRequest,
    ): Response<AtomicPaymentMutationResponse>

    @POST("orders/{id}/manual-payments")
    suspend fun recordManualPayment(
        @Path("id") id: Int,
        @Body payload: RecordManualPaymentRequest,
    ): Response<AtomicPaymentMutationResponse>

    @POST("receivables/{id}/confirm-payment")
    suspend fun confirmPayment(@Path("id") id: String, @Body payload: ConfirmPaymentRequest): Response<CreateOrderResponse>

    @POST("receivables/{id}/generate-pix")
    suspend fun generatePixCharge(
        @Path("id") id: String,
        @Body payload: PixChargeRequest
    ): Response<PixChargeResponse>

    @PUT("order-receivables/{id}")
    suspend fun refundOrderReceivableItem(@Path("id") id: String, @Body payload: RefundOrderReceivableRequest): Response<OrderReceivableMutationResponse>

    @PUT("order-receivables/{id}")
    suspend fun updateOrderReceivableItem(@Path("id") id: String, @Body payload: UpdateOrderReceivableRequest): Response<OrderReceivableMutationResponse>

    @DELETE("receivables/{id}")
    suspend fun deleteOrderReceivableItem(@Path("id") id: String): Response<OrderReceivableMutationResponse>

    @POST("update-split-config")
    suspend fun updateSplitConfig(@Body payload: SplitConfigRequest): Response<Unit>

    @GET
    suspend fun downloadFile(@Url url: String): Response<ResponseBody>

}
