package com.livraison.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livraison.client.data.AppUiState

@Composable
fun EstimateScreen(
    uiState: AppUiState,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Estimation de la course", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Départ : ${uiState.pickupAddress}")
                Spacer(Modifier.height(6.dp))
                Text("Arrivée : ${uiState.dropoffAddress}")
                Spacer(Modifier.height(6.dp))
                Text("Colis : ${uiState.parcelType.label}")
                Spacer(Modifier.height(6.dp))
                Text("Distance estimée : ${"%.1f".format(uiState.estimatedDistanceKm)} km")
                Spacer(Modifier.height(16.dp))
                Text(
                    "${uiState.estimatedPrice} FCFA",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Prix calculé par le serveur", fontSize = 12.sp)
            }
        }
        if (uiState.errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onConfirm,
            enabled = !uiState.isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (uiState.isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Confirmer la commande")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour")
        }
    }
}
