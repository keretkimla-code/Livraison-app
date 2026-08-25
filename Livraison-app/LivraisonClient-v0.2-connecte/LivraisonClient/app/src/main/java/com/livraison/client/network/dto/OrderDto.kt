package com.livraison.client.network.dto

import com.google.gson.annotations.SerializedName

data class OrderEstimateRequest(
    @SerializedName("pickup_lat") val pickupLat: Double,
    @SerializedName("pickup_lng") val pickupLng: Double,
    @SerializedName("dropoff_lat") val dropoffLat: Double,
    @SerializedName("dropoff_lng") val dropoffLng: Double,
    @SerializedName("parcel_type") val parcelType: String
)

data class OrderEstimateResponse(
    @SerializedName("distance_km") val distanceKm: Double,
    val price: Int
)

data class OrderCreateRequest(
    @SerializedName("pickup_address") val pickupAddress: String,
    @SerializedName("pickup_lat") val pickupLat: Double,
    @SerializedName("pickup_lng") val pickupLng: Double,
    @SerializedName("dropoff_address") val dropoffAddress: String,
    @SerializedName("dropoff_lat") val dropoffLat: Double,
    @SerializedName("dropoff_lng") val dropoffLng: Double,
    @SerializedName("parcel_type") val parcelType: String
)

data class PayOrderRequest(val method: String)

data class RateOrderRequest(val rating: Int, val comment: String? = null)

data class OrderResponse(
    val id: String,
    @SerializedName("client_id") val clientId: String,
    @SerializedName("courier_id") val courierId: String?,
    @SerializedName("pickup_address") val pickupAddress: String,
    @SerializedName("pickup_lat") val pickupLat: Double,
    @SerializedName("pickup_lng") val pickupLng: Double,
    @SerializedName("dropoff_address") val dropoffAddress: String,
    @SerializedName("dropoff_lat") val dropoffLat: Double,
    @SerializedName("dropoff_lng") val dropoffLng: Double,
    @SerializedName("parcel_type") val parcelType: String,
    @SerializedName("distance_km") val distanceKm: Double,
    val price: Int,
    val status: String,
    @SerializedName("payment_method") val paymentMethod: String?,
    @SerializedName("delivery_code") val deliveryCode: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)
