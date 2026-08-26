package com.livraison.client.network.dto

import com.google.gson.annotations.SerializedName

data class GeocodeResult(
    @SerializedName("display_name") val displayName: String,
    val lat: Double,
    val lng: Double
)
