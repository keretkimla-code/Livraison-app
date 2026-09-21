package com.livraison.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livraison.client.data.AppUiState

private val paymentMethods = listOf("Airtel Money", "Moov Money", "Cash à la livraison")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    uiState: AppUiState,
    onPay: (String) -> Unit,
    onBack: () -> Unit
) {
    var selected by remember { mutableStateOf(paymentMethods.first()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paiement") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text("Montant à payer : ${uiState.currentOrder?.price ?: 0} FCFA", fontSize = 16.sp)
            Spacer(Modifier.height(24.dp))

            paymentMethods.forEach { method ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected == method,
                            onClick = { selected = method }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected == method, onClick = { selected = method })
                    Spacer(Modifier.width(8.dp))
                    Text(method)
                }
            }

            if (uiState.errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { onPay(selected) },
                enabled = !uiState.isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Payer maintenant")
                }
            }
        }
    }
}