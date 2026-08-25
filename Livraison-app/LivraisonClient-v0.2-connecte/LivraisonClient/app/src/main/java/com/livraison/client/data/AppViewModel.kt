package com.livraison.client.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livraison.client.data.model.ChatMessage
import com.livraison.client.data.model.DeliveryOrder
import com.livraison.client.data.model.OrderStatus
import com.livraison.client.data.model.ParcelType
import com.livraison.client.network.RetrofitClient
import com.livraison.client.network.dto.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * État global de l'application, connecté au backend FastAPI via Retrofit.
 *
 * Ce qui reste simulé côté app (documenté ici et dans le README) :
 * - les coordonnées GPS des adresses saisies (générées aléatoirement
 *   autour de N'Djamena, faute d'intégration d'un vrai service de
 *   géocodage/carte — à remplacer par Google Maps Places API ou
 *   Nominatim/OSM en V1)
 * - la position du livreur sur la carte pendant le suivi, déduite du
 *   statut de la commande (pas de flux GPS temps réel pour l'instant —
 *   prévu via WebSocket en V1)
 */
data class AppUiState(
    val phoneNumber: String = "",
    val otpSent: Boolean = false,
    val isAuthenticated: Boolean = false,
    val fullName: String = "",
    val pickupAddress: String = "",
    val dropoffAddress: String = "",
    val parcelType: ParcelType = ParcelType.COLIS_LEGER,
    val estimatedPrice: Int = 0,
    val estimatedDistanceKm: Double = 0.0,
    val currentOrder: DeliveryOrder? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val orderHistory: List<DeliveryOrder> = emptyList(),
    val isBusy: Boolean = false,
    val errorMessage: String? = null
)

class AppViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState

    private val api get() = RetrofitClient.service
    private val random = Random(System.currentTimeMillis())

    // Coordonnées approximatives associées à la dernière estimation, pour
    // réutilisation lors de la création de la commande.
    private var pendingPickupLat = 0.0
    private var pendingPickupLng = 0.0
    private var pendingDropoffLat = 0.0
    private var pendingDropoffLng = 0.0

    private var pollingJob: kotlinx.coroutines.Job? = null

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun setBusy(value: Boolean) {
        _uiState.update { it.copy(isBusy = value) }
    }

    // --- Auth ---

    fun updatePhoneNumber(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone) }
    }

    fun sendOtp() {
        viewModelScope.launch {
            setBusy(true)
            try {
                api.sendOtp(SendOtpRequest(_uiState.value.phoneNumber))
                _uiState.update { it.copy(otpSent = true, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Erreur réseau") }
            } finally {
                setBusy(false)
            }
        }
    }

    fun verifyOtp(code: String, fullName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            setBusy(true)
            try {
                val response = api.verifyOtp(
                    VerifyOtpRequest(
                        phone = _uiState.value.phoneNumber,
                        code = code,
                        role = "client",
                        fullName = fullName
                    )
                )
                RetrofitClient.setToken(response.accessToken)
                _uiState.update {
                    it.copy(isAuthenticated = true, fullName = fullName, errorMessage = null)
                }
                onResult(true)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Code invalide") }
                onResult(false)
            } finally {
                setBusy(false)
            }
        }
    }

    // --- Création de commande ---

    fun updatePickup(address: String) = _uiState.update { it.copy(pickupAddress = address) }
    fun updateDropoff(address: String) = _uiState.update { it.copy(dropoffAddress = address) }
    fun updateParcelType(type: ParcelType) = _uiState.update { it.copy(parcelType = type) }

    /**
     * Génère des coordonnées plausibles autour de N'Djamena pour une
     * adresse texte donnée (faute de géocodage réel en bêta).
     */
    private fun fakeCoordinatesFor(seed: String): Pair<Double, Double> {
        val r = Random(seed.hashCode())
        val lat = 12.1348 + (r.nextDouble() - 0.5) * 0.08
        val lng = 15.0557 + (r.nextDouble() - 0.5) * 0.08
        return lat to lng
    }

    fun estimatePrice() {
        viewModelScope.launch {
            setBusy(true)
            try {
                val (pLat, pLng) = fakeCoordinatesFor(_uiState.value.pickupAddress)
                val (dLat, dLng) = fakeCoordinatesFor(_uiState.value.dropoffAddress)
                pendingPickupLat = pLat; pendingPickupLng = pLng
                pendingDropoffLat = dLat; pendingDropoffLng = dLng

                val response = api.estimateOrder(
                    OrderEstimateRequest(pLat, pLng, dLat, dLng, _uiState.value.parcelType.apiValue)
                )
                _uiState.update {
                    it.copy(
                        estimatedPrice = response.price,
                        estimatedDistanceKm = response.distanceKm,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Erreur réseau") }
            } finally {
                setBusy(false)
            }
        }
    }

    fun confirmOrder(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            setBusy(true)
            try {
                val state = _uiState.value
                val response = api.createOrder(
                    OrderCreateRequest(
                        pickupAddress = state.pickupAddress,
                        pickupLat = pendingPickupLat,
                        pickupLng = pendingPickupLng,
                        dropoffAddress = state.dropoffAddress,
                        dropoffLat = pendingDropoffLat,
                        dropoffLng = pendingDropoffLng,
                        parcelType = state.parcelType.apiValue
                    )
                )
                _uiState.update {
                    it.copy(currentOrder = DeliveryOrder.fromResponse(response), errorMessage = null)
                }
                startOrderPolling()
                onResult(true)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Erreur réseau") }
                onResult(false)
            } finally {
                setBusy(false)
            }
        }
    }

    // --- Suivi de la commande (interrogation périodique du statut) ---

    private fun startOrderPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                val orderId = _uiState.value.currentOrder?.id ?: break
                try {
                    val response = api.getOrder(orderId)
                    val order = DeliveryOrder.fromResponse(response)
                    _uiState.update { it.copy(currentOrder = order) }
                    if (order.status == OrderStatus.AT_DROPOFF ||
                        order.status == OrderStatus.DELIVERED ||
                        order.status == OrderStatus.PAID
                    ) {
                        break
                    }
                } catch (e: Exception) {
                    // On ignore une erreur ponctuelle de polling pour ne pas
                    // interrompre le suivi ; elle réapparaîtra si persistante.
                }
            }
        }
    }

    private fun stopOrderPolling() {
        pollingJob?.cancel()
    }

    // --- Chat ---

    fun loadChatMessages() {
        val orderId = _uiState.value.currentOrder?.id ?: return
        viewModelScope.launch {
            try {
                val messages = api.getMessages(orderId)
                _uiState.update {
                    it.copy(
                        chatMessages = messages.map { m ->
                            ChatMessage(text = m.text, fromCustomer = m.senderRole == "client")
                        }
                    )
                }
            } catch (_: Exception) {
                // silencieux : le chat réessaiera au prochain rafraîchissement
            }
        }
    }

    fun sendChatMessage(text: String) {
        val orderId = _uiState.value.currentOrder?.id ?: return
        viewModelScope.launch {
            try {
                api.sendMessage(orderId, ChatMessageIn(text))
                loadChatMessages()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Erreur d'envoi") }
            }
        }
    }

    // --- Paiement ---

    fun payOrder(method: String, onResult: (Boolean) -> Unit) {
        val orderId = _uiState.value.currentOrder?.id ?: return
        viewModelScope.launch {
            setBusy(true)
            try {
                val response = api.payOrder(orderId, PayOrderRequest(method))
                _uiState.update { it.copy(currentOrder = DeliveryOrder.fromResponse(response), errorMessage = null) }
                stopOrderPolling()
                onResult(true)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Erreur de paiement") }
                onResult(false)
            } finally {
                setBusy(false)
            }
        }
    }

    // --- Historique et notation ---

    fun loadHistory() {
        viewModelScope.launch {
            try {
                val orders = api.getOrderHistory()
                _uiState.update { it.copy(orderHistory = orders.map(DeliveryOrder::fromResponse)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Erreur réseau") }
            }
        }
    }

    fun rateOrder(orderId: String, rating: Int) {
        viewModelScope.launch {
            try {
                api.rateOrder(orderId, RateOrderRequest(rating))
                loadHistory()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Erreur réseau") }
            }
        }
    }

    fun resetCurrentOrder() {
        stopOrderPolling()
        _uiState.update {
            it.copy(
                currentOrder = null,
                chatMessages = emptyList(),
                pickupAddress = "",
                dropoffAddress = ""
            )
        }
    }

    override fun onCleared() {
        stopOrderPolling()
        super.onCleared()
    }
}
