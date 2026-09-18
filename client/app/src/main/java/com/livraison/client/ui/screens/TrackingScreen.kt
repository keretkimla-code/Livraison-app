package com.livraison.client.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livraison.client.data.AppUiState
import com.livraison.client.data.model.OrderStatus
import kotlin.math.*

@Composable
fun TrackingScreen(
    uiState: AppUiState,
    onOpenChat: () -> Unit,
    onProceedToPayment: () -> Unit
) {
    val order = uiState.currentOrder

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val start = Offset(size.width * 0.15f, size.height * 0.85f)
                val end = Offset(size.width * 0.85f, size.height * 0.15f)

                drawLine(
                    color = Color(0xFFBBBBBB),
                    start = start,
                    end = end,
                    strokeWidth = 6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
                )
                drawCircle(color = Color(0xFF2E7D32), radius = 18f, center = start)
                drawCircle(color = Color(0xFFC62828), radius = 18f, center = end)

                val progress = order?.let { OrderStatus.progressFor(it.status) } ?: 0f
                val courierPos = Offset(
                    x = start.x + (end.x - start.x) * progress,
                    y = start.y + (end.y - start.y) * progress
                )
                drawCircle(color = Color(0xFF1565C0), radius = 24f, center = courierPos)
                drawCircle(color = Color.White, radius = 10f, center = courierPos)
            }
            val distanceText = if (order?.courierLat != null && order.courierLng != null) {
                val km = haversineKm(order.courierLat, order.courierLng, order.dropoffLat, order.dropoffLng)
                "Livreur à %.1f km de l'arrivée (position GPS réelle)".format(km)
            } else {
                "Position du livreur non disponible pour le moment"
            }
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

private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}