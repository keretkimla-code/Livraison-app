package com.livraison.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.livraison.client.data.AppUiState
import com.livraison.client.data.model.OrderStatus
import kotlin.math.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    uiState: AppUiState,
    onOpenChat: () -> Unit,
    onProceedToPayment: () -> Unit,
    onMinimize: () -> Unit
) {
    val order = uiState.currentOrder

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suivi de la commande") },
                navigationIcon = {
                    IconButton(onClick = onMinimize) {
                        Icon(Icons.Filled.Home, contentDescription = "Retour à l'accueil")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val distanceText = if (order?.courierLat != null && order.courierLng != null) {
                    val km = haversineKm(order.courierLat, order.courierLng, order.dropoffLat, order.dropoffLng)
                    "Livreur à %.1f km de l'arrivée (position GPS réelle)".format(km)
                } else {
                    "Position du livreur non disponible pour le moment"
                }
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        Configuration.getInstance().userAgentValue = context.packageName
                        MapView(context).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(14.0)
                        }
                    },
                    update = { mapView ->
                        mapView.overlays.clear()
                        if (order != null) {
                            val pickup = GeoPoint(order.pickupLat, order.pickupLng)
                            val dropoff = GeoPoint(order.dropoffLat, order.dropoffLng)

                            val pickupMarker = Marker(mapView)
                            pickupMarker.position = pickup
                            pickupMarker.title = "Départ"
                            mapView.overlays.add(pickupMarker)

                            val dropoffMarker = Marker(mapView)
                            dropoffMarker.position = dropoff
                            dropoffMarker.title = "Arrivée"
                            mapView.overlays.add(dropoffMarker)

                            if (order.courierLat != null && order.courierLng != null) {
                                val courierPoint = GeoPoint(order.courierLat, order.courierLng)
                                val courierMarker = Marker(mapView)
                                courierMarker.position = courierPoint
                                courierMarker.title = "Livreur"
                                mapView.overlays.add(courierMarker)
                                mapView.controller.setCenter(courierPoint)
                            } else {
                                mapView.controller.setCenter(pickup)
                            }
                        }
                        mapView.invalidate()
                    }
                )
                Surface(
                    color = Color.White,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                ) {
                    Text(
                        distanceText,
                        fontSize = 11.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        order?.status?.label ?: "En attente",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Commande : ${order?.id ?: "—"} · ${order?.price ?: 0} FCFA")

                    if (order?.deliveryCode != null) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Code à donner au livreur à la livraison", fontSize = 12.sp)
                                Text(
                                    order.deliveryCode,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onOpenChat, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Chat avec le livreur")
                    }

                    if (order?.status == OrderStatus.AT_DROPOFF || order?.status == OrderStatus.DELIVERED) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onProceedToPayment,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Procéder au paiement")
                        }
                    }
                }
            }
        }
    }
}

private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}