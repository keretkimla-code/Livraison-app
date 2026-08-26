package com.livraison.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livraison.client.data.AppUiState
import com.livraison.client.data.model.ParcelType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: AppUiState,
    onPickupChange: (String) -> Unit,
    onDropoffChange: (String) -> Unit,
    onParcelTypeChange: (ParcelType) -> Unit,
    onEstimate: () -> Unit,
    onOpenHistory: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle livraison") },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = "Historique")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize()
        ) {
            if (uiState.errorMessage != null) {
                Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            }
            Text("D'où partons-nous ?", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.pickupAddress,
                onValueChange = onPickupChange,
                label = { Text("Adresse de départ") },
                placeholder = { Text("Ex : Quartier Klemat, Rue 32") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Text("Où livrons-nous ?", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.dropoffAddress,
                onValueChange = onDropoffChange,
                label = { Text("Adresse d'arrivée") },
                placeholder = { Text("Ex : Avenue Charles de Gaulle") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Text("Type de colis", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.parcelType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type de colis") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ParcelType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                onParcelTypeChange(type)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onEstimate,
                enabled = uiState.pickupAddress.isNotBlank() && uiState.dropoffAddress.isNotBlank() && !uiState.isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Estimer le prix")
                }
            }
        }
    }
}
