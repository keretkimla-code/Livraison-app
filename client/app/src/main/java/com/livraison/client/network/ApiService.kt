package com.livraison.client.network

import com.livraison.client.network.dto.*
import retrofit2.http.*

interface ApiService {

    @POST("auth/send-otp")
    suspend fun sendOtp(@Body request: SendOtpRequest): Map<String, Any?>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): TokenResponse

    @POST("orders/estimate")
    suspend fun estimateOrder(@Body request: OrderEstimateRequest): OrderEstimateResponse

    @POST("orders")
    suspend fun createOrder(@Body request: OrderCreateRequest): OrderResponse

    @GET("orders/{orderId}")
    suspend fun getOrder(@Path("orderId") orderId: String): OrderResponse

    @GET("orders/history")
    suspend fun getOrderHistory(): List<OrderResponse>

    @POST("orders/{orderId}/pay")
    suspend fun payOrder(@Path("orderId") orderId: String, @Body request: PayOrderRequest): OrderResponse

    @POST("orders/{orderId}/rate")
    suspend fun rateOrder(@Path("orderId") orderId: String, @Body request: RateOrderRequest): Map<String, Any?>

    @GET("orders/{orderId}/messages")
    suspend fun getMessages(@Path("orderId") orderId: String): List<ChatMessageResponse>

    @POST("orders/{orderId}/messages")
    suspend fun sendMessage(@Path("orderId") orderId: String, @Body request: ChatMessageIn): ChatMessageResponse

    @GET("geocode/search")
    suspend fun searchAddress(@Query("q") query: String): List<GeocodeResult>
}
