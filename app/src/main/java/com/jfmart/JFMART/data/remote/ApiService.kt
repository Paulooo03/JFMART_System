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

    @GET("api/clients/me")
    suspend fun profile(): ClientProfile

    @PUT("api/clients/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ClientProfile
}
