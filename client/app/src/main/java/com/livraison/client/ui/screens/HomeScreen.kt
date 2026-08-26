package com.livraison.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livraison.client.data.AppUiState
import com.livraison.client.data.model.ParcelType
import com.livraison.client.network.dto.GeocodeResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: AppUiState,
    onPickupQueryChange: (String) -> Unit,
    onPickupSuggestionSelected: (GeocodeResult) -> Unit,
    onDropoffQueryChange: (String) -> Unit,
    onDropoffSuggestionSelected: (GeocodeResult) -> Unit,
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
                Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            }

            Text("D'où partons-nous ?", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            AddressAutocompleteField(
                value = uiState.pickupAddress,
                onValueChange = onPickupQueryChange,
                placeholder = "Ex : Quartier Klemat, N'Djamena",
                suggestions = uiState.pickupSuggestions,
                isSearching = uiState.isSearchingPickup,
                isConfirmed = uiState.pickupLat != null,
                onSuggestionSelected = onPickupSuggestionSelected
            )

            Spacer(Modifier.height(16.dp))
            Text("Où livrons-nous ?", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            AddressAutocompleteField(
                value = uiState.dropoffAddress,
                onValueChange = onDropoffQueryChange,
                placeholder = "Ex : Avenue Charles de Gaulle",
                suggestions = uiState.dropoffSuggestions,
                isSearching = uiState.isSearchingDropoff,
                isConfirmed = uiState.dropoffLat != null,
                onSuggestionSelected = onDropoffSuggestionSelected
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
                enabled = uiState.addressesReady && !uiState.isBusy,
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

/**
 * Champ d'adresse avec autocomplétion : affiche une liste de suggestions
 * sous le champ pendant la saisie (issues du géocodage réel côté
 * backend). Tant qu'aucune suggestion n'a été sélectionnée, l'adresse
 * n'a pas de coordonnées GPS valides.
 */
@Composable
private fun AddressAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    suggestions: List<GeocodeResult>,
    isSearching: Boolean,
    isConfirmed: Boolean,
    onSuggestionSelected: (GeocodeResult) -> Unit
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            trailingIcon = {
                when {
                    isSearching -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    isConfirmed -> Icon(Icons.Filled.LocationOn, contentDescription = "Adresse confirmée")
                    else -> null
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (suggestions.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(suggestions) { suggestion ->
                        Text(
                            suggestion.displayName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onSuggestionSelected(suggestion) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else if (!isConfirmed && value.length in 1..2) {
            Text(
                "Continue à taper (min. 3 caractères) pour voir des suggestions",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}
