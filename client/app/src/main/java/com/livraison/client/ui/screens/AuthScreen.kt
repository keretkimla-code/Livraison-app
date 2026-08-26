package com.livraison.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livraison.client.data.AppUiState

@Composable
fun AuthScreen(
    uiState: AppUiState,
    onPhoneChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onVerifyOtp: (String, String) -> Unit,
    onAuthenticated: () -> Unit
) {
    var otpInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onAuthenticated()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(8.dp))
        Text("Livraison Client", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Version bêta — connectée au serveur", fontSize = 13.sp)
        Spacer(Modifier.height(32.dp))

        uiState.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
        }

        if (!uiState.otpSent) {
            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = onPhoneChange,
                label = { Text("Numéro de téléphone") },
                placeholder = { Text("+235 66 00 00 00") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSendOtp,
                enabled = uiState.phoneNumber.length >= 8 && !uiState.isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Recevoir le code OTP")
                }
            }
        } else {
            Text("Un code à 4 chiffres a été envoyé au ${uiState.phoneNumber}")
            Text("(Mode bêta : utilisez 0000 pour tester)", fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Nom complet") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = otpInput,
                onValueChange = { if (it.length <= 4) otpInput = it },
                label = { Text("Code OTP") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onVerifyOtp(otpInput, nameInput) },
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Vérifier")
                }
            }
        }
    }
}
