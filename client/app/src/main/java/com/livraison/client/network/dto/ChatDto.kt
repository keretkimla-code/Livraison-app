package com.livraison.client.network.dto

import com.google.gson.annotations.SerializedName

data class ChatMessageIn(val text: String)

data class ChatMessageResponse(
    val id: String,
    @SerializedName("order_id") val orderId: String,
    @SerializedName("sender_role") val senderRole: String,
    val text: String,
    @SerializedName("created_at") val createdAt: String
)
