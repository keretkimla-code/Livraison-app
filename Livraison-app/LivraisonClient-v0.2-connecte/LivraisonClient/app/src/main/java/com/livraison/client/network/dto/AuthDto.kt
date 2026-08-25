package com.livraison.client.network.dto

import com.google.gson.annotations.SerializedName

data class SendOtpRequest(val phone: String)

data class VerifyOtpRequest(
    val phone: String,
    val code: String,
    val role: String, // "client"
    @SerializedName("full_name") val fullName: String?
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("user_id") val userId: String,
    val role: String
)
