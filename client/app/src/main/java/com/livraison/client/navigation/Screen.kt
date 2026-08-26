package com.livraison.client.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Estimate : Screen("estimate")
    object Tracking : Screen("tracking")
    object Chat : Screen("chat")
    object Payment : Screen("payment")
    object History : Screen("history")
}
