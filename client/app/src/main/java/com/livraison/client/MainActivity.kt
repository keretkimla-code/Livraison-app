package com.livraison.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.livraison.client.data.AppViewModel
import com.livraison.client.navigation.Screen
import com.livraison.client.ui.screens.*
import com.livraison.client.ui.theme.LivraisonClientTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LivraisonClientTheme {
                LivraisonApp()
            }
        }
    }
}

@Composable
fun LivraisonApp() {
    val navController = rememberNavController()
    val viewModel: AppViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = Screen.Auth.route) {
        composable(Screen.Auth.route) {
            AuthScreen(
                uiState = uiState,
                onPhoneChange = viewModel::updatePhoneNumber,
                onSendOtp = viewModel::sendOtp,
                onVerifyOtp = { code, name -> viewModel.verifyOtp(code, name) {} },
                onAuthenticated = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                uiState = uiState,
                onPickupQueryChange = viewModel::updatePickupQuery,
                onPickupSuggestionSelected = viewModel::selectPickupSuggestion,
                onDropoffQueryChange = viewModel::updateDropoffQuery,
                onDropoffSuggestionSelected = viewModel::selectDropoffSuggestion,
                onParcelTypeChange = viewModel::updateParcelType,
                onEstimate = {
                    viewModel.estimatePrice()
                    navController.navigate(Screen.Estimate.route)
                },
                onOpenHistory = { navController.navigate(Screen.History.route) }
            )
        }
        composable(Screen.Estimate.route) {
            EstimateScreen(
                uiState = uiState,
                onConfirm = {
                    viewModel.confirmOrder { success ->
                        if (success) {
                            navController.navigate(Screen.Tracking.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Tracking.route) {
            TrackingScreen(
                uiState = uiState,
                onOpenChat = { navController.navigate(Screen.Chat.route) },
                onProceedToPayment = { navController.navigate(Screen.Payment.route) }
            )
        }
        composable(Screen.Chat.route) {
            ChatScreen(
                uiState = uiState,
                onLoadMessages = viewModel::loadChatMessages,
                onSendMessage = viewModel::sendChatMessage,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Payment.route) {
            PaymentScreen(
                uiState = uiState,
                onPay = { method ->
                    viewModel.payOrder(method) { success ->
                        if (success) {
                            viewModel.resetCurrentOrder()
                            navController.navigate(Screen.History.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    }
                }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                orders = uiState.orderHistory,
                onLoadHistory = viewModel::loadHistory,
                onRate = viewModel::rateOrder,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
