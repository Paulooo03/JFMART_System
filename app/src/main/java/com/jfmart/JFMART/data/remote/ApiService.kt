package com.jfmart.JFMART.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/signup")
    suspend fun signUp(@Body request: SignUpRequest)

    @GET("api/products")
    suspend fun products(@Query("search") search: String): List<Product>

    @POST("api/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Order

    @GET("api/orders/mine")
    suspend fun myOrders(): List<Order>

    @GET("api/orders/{id}")
    suspend fun order(@Path("id") id: Long): Order

    @POST("api/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: Long, @Body request: CancelRequest): Order

    @GET("api/orders/{id}/deliveries")
    suspend fun orderDeliveries(@Path("id") id: Long): List<DeliveryInfo>

    @GET("api/statement/mine")
    suspend fun statement(): Statement

    @POST("api/deliveries/confirm")
    suspend fun confirmDelivery(@Body request: ConfirmDeliveryRequest): DeliveryConfirmed

    @GET("api/courier/deliveries")
    suspend fun courierDeliveries(): List<CourierDelivery>

    @GET("api/courier/deliveries/{id}")
    suspend fun courierDelivery(@Path("id") id: Long): CourierDelivery

    @POST("api/courier/deliveries/{id}/preparing")
    suspend fun courierPreparing(@Path("id") id: Long): CourierDelivery

    @POST("api/courier/deliveries/{id}/out-for-delivery")
    suspend fun courierOutForDelivery(@Path("id") id: Long): CourierDelivery

    @POST("api/courier/deliveries/{id}/qr")
    suspend fun courierQr(@Path("id") id: Long): QrToken

    @POST("api/courier/deliveries/{id}/unsuccessful")
    suspend fun courierUnsuccessful(@Path("id") id: Long, @Body request: UnsuccessfulRequest): CourierDelivery

    @GET("api/clients/me")
    suspend fun profile(): ClientProfile

    @PUT("api/clients/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ClientProfile
}
