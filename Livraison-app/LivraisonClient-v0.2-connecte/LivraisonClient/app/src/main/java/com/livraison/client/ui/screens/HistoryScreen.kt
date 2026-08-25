package com.livraison.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livraison.client.data.model.DeliveryOrder
import com.livraison.client.data.model.OrderStatus

@Composable
fun HistoryScreen(
    orders: List<DeliveryOrder>,
    onLoadHistory: () -> Unit,
    onRate: (String, Int) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { onLoadHistory() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique des commandes") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Retour") } }
            )
        }
    ) { padding ->
        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucune commande pour le moment.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(12.dp)
            ) {
                items(orders) { order ->
                    OrderHistoryCard(order = order, onRate = onRate)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun OrderHistoryCard(order: DeliveryOrder, onRate: (String, Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(order.id, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("${order.pickupAddress} → ${order.dropoffAddress}", fontSize = 13.sp)
            Text("Colis : ${order.parcelType.label}", fontSize = 13.sp)
            Text("Statut : ${order.status.label}", fontSize = 13.sp)
            Text("Prix : ${order.price} FCFA · ${order.paymentMethod ?: ""}", fontSize = 13.sp)
            if (order.status == OrderStatus.PAID) {
                Spacer(Modifier.height(8.dp))
                Text("Note du livreur :", fontSize = 13.sp)
                Row {
                    for (i in 1..5) {
                        IconButton(onClick = { onRate(order.id, i) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}
