package com.livraison.client.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livraison.client.data.model.ChatMessage
import com.livraison.client.data.model.DeliveryOrder
import com.livraison.client.data.model.OrderStatus
import com.livraison.client.data.model.ParcelType
import com.livraison.client.network.RetrofitClient
import com.livraison.client.network.dto.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * État global de l'application, connecté au backend FastAPI via Retrofit.
 *
 * Ce qui reste simulé côté app (documenté ici et dans le README) :
 * - la position du livreur sur la carte pendant le suivi, déduite du
 *   statut de la commande (pas de flux GPS temps réel pour l'instant —
 *   prévu via WebSocket en V1, l'endpoint /ws/orders/{id} existe déjà
 *   côté backend)
 *
 * Le géocodage des adresses (recherche + coordonnées GPS) est désormais
 * réel, via l'endpoint /geocode/search du backend (proxy Nominatim/OSM).
 */
data class AppUiState(
    val phoneNumber: String = "",
    val otpSent: Boolean = false,
    val isAuthenticated: Boolean = false,
    val fullName: String = "",

    val pickupAddress: String = "",
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val pickupSuggestions: List<GeocodeResult> = emptyList(),
    val isSearchingPickup: Boolean = false,

    val dropoffAddress: String = "",
    val dropoffLat: Double? = null,
    val dropoffLng: Double? = null,
    val dropoffSuggestions: List<GeocodeResult> = emptyList(),
    val isSearchingDropoff: Boolean = false,

    val parcelType: ParcelType = ParcelType.COLIS_LEGER,
    val estimatedPrice: Int = 0,
    val estimatedDistanceKm: Double = 0.0,
    val currentOrder: DeliveryOrder? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val orderHistory: List<DeliveryOrder> = emptyList(),
    val isBusy: Boolean = false,
    val errorMessage: String? = null
) {
    /** La commande ne peut être estimée que si les deux adresses ont été choisies dans les suggestions. */
    val addressesReady: Boolean
        get() = pickupLat != null && pickupLng != null && dropoffLat != null && dropoffLng != null
}

class AppViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState

    private val api get() = RetrofitClient.service

    private var pollingJob: Job? = null
    private var pickupSearchJob: Job? = null
    private var dropoffSearchJob: Job? = null

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

    // --- Recherche d'adresse (géocodage réel via le backend) ---

    fun updatePickupQuery(text: String) {
        _uiState.update {
            it.copy(
                pickupAddress = text,
                pickupLat = null,
                pickupLng = null,
                pickupSuggestions = emptyList()
            )
        }
        pickupSearchJob?.cancel()
        if (text.trim().length < 3) return
        pickupSearchJob = viewModelScope.launch {
            delay(400) // anti-rebond : évite un appel réseau à chaque frappe
            _uiState.update { it.copy(isSearchingPickup = true) }
            try {
                val results = api.searchAddress(text)
                _uiState.update { it.copy(pickupSuggestions = results, isSearchingPickup = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSearchingPickup = false, errorMessage = e.message ?: "Erreur de recherche d'adresse")
                }
            }
        }
    }

    fun selectPickupSuggestion(result: GeocodeResult) {
        pickupSearchJob?.cancel()
        _uiState.update {
            it.copy(
                pickupAddress = result.displayName,
                pickupLat = result.lat,
                pickupLng = result.lng,
                pickupSuggestions = emptyList()
            )
        }
    }

    fun updateDropoffQuery(text: String) {
        _uiState.update {
            it.copy(
                dropoffAddress = text,
                dropoffLat = null,
                dropoffLng = null,
                dropoffSuggestions = emptyList()
            )
        }
        dropoffSearchJob?.cancel()
        if (text.trim().length < 3) return
        dropoffSearchJob = viewModelScope.launch {
            delay(400)
            _uiState.update { it.copy(isSearchingDropoff = true) }
            try {
                val results = api.searchAddress(text)
                _uiState.update { it.copy(dropoffSuggestions = results, isSearchingDropoff = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSearchingDropoff = false, errorMessage = e.message ?: "Erreur de recherche d'adresse")
                }
            }
        }
    }

    fun selectDropoffSuggestion(result: GeocodeResult) {
        dropoffSearchJob?.cancel()
        _uiState.update {
            it.copy(
                dropoffAddress = result.displayName,
                dropoffLat = result.lat,
                dropoffLng = result.lng,
                dropoffSuggestions = emptyList()
            )
        }
    }

    fun updateParcelType(type: ParcelType) = _uiState.update { it.copy(parcelType = type) }

    // --- Estimation et création de commande ---

    fun estimatePrice() {
        val state = _uiState.value
        val pLat = state.pickupLat
        val pLng = state.pickupLng
        val dLat = state.dropoffLat
        val dLng = state.dropoffLng
        if (pLat == null || pLng == null || dLat == null || dLng == null) {
            _uiState.update { it.copy(errorMessage = "Choisis une adresse dans la liste de suggestions.") }
            return
        }
        viewModelScope.launch {
            setBusy(true)
            try {
                val response = api.estimateOrder(
                    OrderEstimateRequest(pLat, pLng, dLat, dLng, state.parcelType.apiValue)
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
        val state = _uiState.value
        val pLat = state.pickupLat
        val pLng = state.pickupLng
        val dLat = state.dropoffLat
        val dLng = state.dropoffLng
        if (pLat == null || pLng == null || dLat == null || dLng == null) {
            _uiState.update { it.copy(errorMessage = "Choisis une adresse dans la liste de suggestions.") }
            onResult(false)
            return
        }
        viewModelScope.launch {
            setBusy(true)
            try {
                val response = api.createOrder(
                    OrderCreateRequest(
                        pickupAddress = state.pickupAddress,
                        pickupLat = pLat,
                        pickupLng = pLng,
                        dropoffAddress = state.dropoffAddress,
                        dropoffLat = dLat,
                        dropoffLng = dLng,
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
                pickupLat = null,
                pickupLng = null,
                dropoffAddress = "",
                dropoffLat = null,
                dropoffLng = null
            )
        }
    }

    override fun onCleared() {
        stopOrderPolling()
        pickupSearchJob?.cancel()
        dropoffSearchJob?.cancel()
        super.onCleared()
    }
}
