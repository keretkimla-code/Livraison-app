package com.livraison.client.data.model

import com.livraison.client.network.dto.OrderResponse

enum class OrderStatus(val label: String, val apiValue: String) {
    PENDING("En attente", "pending"),
    ACCEPTED("Acceptée", "accepted"),
    HEADING_TO_PICKUP("Le livreur vient récupérer le colis", "heading_to_pickup"),
    AT_PICKUP("Le livreur est arrivé à la collecte", "at_pickup"),
    HEADING_TO_DROPOFF("Le livreur est en route vers vous", "heading_to_dropoff"),
    AT_DROPOFF("Le livreur est arrivé", "at_dropoff"),
    DELIVERED("Livrée", "delivered"),
    PAID("Payée", "paid"),
    CANCELLED("Annulée", "cancelled");

    companion object {
        fun fromApi(value: String): OrderStatus =
            values().firstOrNull { it.apiValue == value } ?: PENDING

        /** Progression approximative sur le trajet, déduite du statut (pas de GPS live pour l'instant). */
        fun progressFor(status: OrderStatus): Float = when (status) {
            PENDING -> 0f
            ACCEPTED, HEADING_TO_PICKUP -> 0.25f
            AT_PICKUP -> 0.5f
            HEADING_TO_DROPOFF -> 0.75f
            AT_DROPOFF, DELIVERED, PAID -> 1f
            CANCELLED -> 0f
        }
    }
}

enum class ParcelType(val label: String, val apiValue: String) {
    DOCUMENT("Document", "document"),
    COLIS_LEGER("Colis léger (< 5kg)", "colis_leger"),
    COLIS_LOURD("Colis lourd (> 5kg)", "colis_lourd"),
    REPAS("Repas", "repas"),
    COURSES("Courses", "courses");

    companion object {
        fun fromApi(value: String): ParcelType =
            values().firstOrNull { it.apiValue == value } ?: COLIS_LEGER
    }
}

data class DeliveryOrder(
    val id: String,
    val clientId: String,
    val courierId: String?,
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropoffAddress: String,
    val dropoffLat: Double,
    val dropoffLng: Double,
    val parcelType: ParcelType,
    val distanceKm: Double,
    val price: Int,
    val status: OrderStatus,
    val paymentMethod: String?,
    val deliveryCode: String?
) {
    companion object {
        fun fromResponse(r: OrderResponse): DeliveryOrder = DeliveryOrder(
            id = r.id,
            clientId = r.clientId,
            courierId = r.courierId,
            pickupAddress = r.pickupAddress,
            pickupLat = r.pickupLat,
            pickupLng = r.pickupLng,
            dropoffAddress = r.dropoffAddress,
            dropoffLat = r.dropoffLat,
            dropoffLng = r.dropoffLng,
            parcelType = ParcelType.fromApi(r.parcelType),
            distanceKm = r.distanceKm,
            price = r.price,
            status = OrderStatus.fromApi(r.status),
            paymentMethod = r.paymentMethod,
            deliveryCode = r.deliveryCode
        )
    }
}

data class ChatMessage(
    val text: String,
    val fromCustomer: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
