import 'dart:async';
import 'dart:math';
import 'package:flutter/foundation.dart';
import '../models/courier.dart';
import '../models/order.dart';
import '../services/api_client.dart';

/// État global de l'application, connecté au backend FastAPI.
///
/// Ce qui reste simulé côté app (documenté dans le README) :
/// - la position GPS du livreur (générée aléatoirement autour de
///   N'Djamena au lieu d'être lue depuis le capteur réel — à remplacer
///   par le package `geolocator` en V1)
/// - l'upload des documents d'identité/véhicule (juste un booléen coché,
///   aucun fichier réellement envoyé — à remplacer par `image_picker` +
///   un endpoint d'upload en V1)
/// - l'animation du trajet sur la carte (dessinée localement pour le
///   retour visuel ; les changements de statut, eux, sont bien envoyés
///   au serveur)
class AppState extends ChangeNotifier {
  final ApiClient _api = ApiClient();
  final Random _random = Random();

  // --- Authentification ---
  String phoneNumber = '';
  bool otpSent = false;
  bool isAuthenticated = false;

  // --- Profil livreur ---
  CourierProfile profile = const CourierProfile();

  // --- Disponibilité / commandes ---
  bool isAvailable = false;
  List<DeliveryOrder> incomingRequests = [];
  DeliveryOrder? currentOrder;
  List<DeliveryOrder> history = [];

  // --- UI ---
  bool isBusy = false;
  String? errorMessage;

  Timer? _pollTimer;
  Timer? _routeTimer;

  void clearError() {
    errorMessage = null;
    notifyListeners();
  }

  void _setBusy(bool value) {
    isBusy = value;
    notifyListeners();
  }

  // --- Auth ---

  void updatePhoneNumber(String phone) {
    phoneNumber = phone;
    notifyListeners();
  }

  Future<bool> sendOtp() async {
    _setBusy(true);
    try {
      await _api.post('/auth/send-otp', {'phone': phoneNumber});
      otpSent = true;
      errorMessage = null;
      return true;
    } catch (e) {
      errorMessage = e.toString();
      return false;
    } finally {
      _setBusy(false);
    }
  }

  Future<bool> verifyOtp(String code, String fullName) async {
    _setBusy(true);
    try {
      final json = await _api.post('/auth/verify-otp', {
        'phone': phoneNumber,
        'code': code,
        'role': 'courier',
        'full_name': fullName,
      });
      _api.setToken(json['access_token'] as String);
      isAuthenticated = true;
      profile = profile.copyWith(fullName: fullName, phoneNumber: phoneNumber);
      errorMessage = null;
      return true;
    } catch (e) {
      errorMessage = e.toString();
      return false;
    } finally {
      _setBusy(false);
    }
  }

  // --- Inscription (dossier livreur) ---

  void updateVehicleType(VehicleType type) {
    profile = profile.copyWith(vehicleType: type);
    notifyListeners();
  }

  void updatePlateNumber(String plate) {
    profile = profile.copyWith(plateNumber: plate);
    notifyListeners();
  }

  void markIdUploaded() {
    profile = profile.copyWith(idUploaded: true);
    notifyListeners();
  }

  void markVehiclePhotoUploaded() {
    profile = profile.copyWith(vehiclePhotoUploaded: true);
    notifyListeners();
  }

  Future<bool> submitRegistration() async {
    _setBusy(true);
    try {
      final json = await _api.post('/couriers/profile', {
        'vehicle_type': profile.vehicleType.apiValue,
        'plate_number': profile.plateNumber,
        'id_document_uploaded': profile.idUploaded,
        'vehicle_photo_uploaded': profile.vehiclePhotoUploaded,
      });
      profile = CourierProfile.fromJson(
        json,
        fullName: profile.fullName,
        phoneNumber: profile.phoneNumber,
      );
      errorMessage = null;
      return true;
    } catch (e) {
      errorMessage = e.toString();
      return false;
    } finally {
      _setBusy(false);
    }
  }

  // --- Disponibilité et position ---

  Future<void> setAvailable(bool value) async {
    _setBusy(true);
    try {
      if (value) {
        // Position GPS simulée autour de N'Djamena (à remplacer par
        // `geolocator` pour une vraie position en V1).
        final lat = 12.1348 + (_random.nextDouble() - 0.5) * 0.05;
        final lng = 15.0557 + (_random.nextDouble() - 0.5) * 0.05;
        await _api.patch('/couriers/me/location', {'lat': lat, 'lng': lng});
      }
      final json = await _api.patch('/couriers/me/availability', {'is_available': value});
      profile = CourierProfile.fromJson(
        json,
        fullName: profile.fullName,
        phoneNumber: profile.phoneNumber,
      );
      isAvailable = value;
      errorMessage = null;
      if (value) {
        _startPolling();
      } else {
        _pollTimer?.cancel();
        incomingRequests = [];
      }
    } catch (e) {
      errorMessage = e.toString();
    } finally {
      _setBusy(false);
    }
  }

  void _startPolling() {
    _pollTimer?.cancel();
    _pollTimer = Timer.periodic(const Duration(seconds: 8), (_) => _refreshNearbyOrders());
    _refreshNearbyOrders();
  }

  Future<void> _refreshNearbyOrders() async {
    if (currentOrder != null) return; // occupé sur une course, on ne rafraîchit pas
    try {
      final json = await _api.get('/orders/nearby');
      incomingRequests = (json as List).map((o) => DeliveryOrder.fromJson(o)).toList();
      notifyListeners();
    } catch (e) {
      // Une erreur de polling ne doit pas interrompre l'app ; on la
      // journalise seulement dans errorMessage sans bloquer l'UI.
      errorMessage = e.toString();
      notifyListeners();
    }
  }

  // --- Acceptation / refus ---

  Future<void> declineRequest(String orderId) async {
    incomingRequests.removeWhere((o) => o.id == orderId);
    notifyListeners();
    try {
      await _api.post('/orders/$orderId/decline');
    } catch (_) {
      // Le refus est surtout indicatif côté serveur (journalisation) ;
      // on ignore silencieusement une éventuelle erreur réseau ici.
    }
  }

  Future<bool> acceptRequest(String orderId) async {
    _setBusy(true);
    try {
      final json = await _api.post('/orders/$orderId/accept');
      currentOrder = DeliveryOrder.fromJson(json);
      incomingRequests = [];
      _pollTimer?.cancel();
      errorMessage = null;
      _startRouteAnimation();
      return true;
    } catch (e) {
      errorMessage = e.toString();
      return false;
    } finally {
      _setBusy(false);
    }
  }

  // --- Trajet (collecte puis livraison) ---

  void _startRouteAnimation() {
    _routeTimer?.cancel();
    _routeTimer = Timer.periodic(const Duration(milliseconds: 700), (timer) async {
      final order = currentOrder;
      if (order == null) {
        timer.cancel();
        return;
      }
      final animating = order.status == OrderStatus.headingToPickup ||
          order.status == OrderStatus.headingToDropoff;
      if (!animating) {
        timer.cancel();
        return;
      }
      order.routeProgress = (order.routeProgress + 0.08).clamp(0, 1);
      notifyListeners();
      if (order.routeProgress >= 1) {
        timer.cancel();
        final nextStatus = order.status == OrderStatus.headingToPickup
            ? OrderStatus.atPickup
            : OrderStatus.atDropoff;
        await _pushStatus(nextStatus, keepProgress: order.routeProgress);
      }
    });
  }

  Future<void> _pushStatus(OrderStatus status, {double keepProgress = 0}) async {
    final order = currentOrder;
    if (order == null) return;
    try {
      final json = await _api.patch('/orders/${order.id}/status', {'status': status.apiValue});
      final updated = DeliveryOrder.fromJson(json);
      updated.routeProgress = keepProgress;
      currentOrder = updated;
      errorMessage = null;
      notifyListeners();
    } catch (e) {
      errorMessage = e.toString();
      notifyListeners();
    }
  }

  Future<void> confirmPickup() async {
    _setBusy(true);
    await _pushStatus(OrderStatus.headingToDropoff, keepProgress: 0);
    _setBusy(false);
    _startRouteAnimation();
  }

  // --- Confirmation de livraison ---

  Future<bool> confirmDelivery({required String code}) async {
    final order = currentOrder;
    if (order == null) return false;
    _setBusy(true);
    try {
      await _api.post('/orders/${order.id}/confirm-delivery', {'code': code});
      currentOrder = null;
      errorMessage = null;
      if (isAvailable) _startPolling();
      return true;
    } catch (e) {
      errorMessage = e.toString();
      return false;
    } finally {
      _setBusy(false);
    }
  }

  // --- Gains et historique ---

  Future<void> refreshProfile() async {
    try {
      final json = await _api.get('/couriers/me');
      profile = CourierProfile.fromJson(
        json,
        fullName: profile.fullName,
        phoneNumber: profile.phoneNumber,
      );
      notifyListeners();
    } catch (e) {
      errorMessage = e.toString();
      notifyListeners();
    }
  }

  Future<void> loadHistory() async {
    try {
      final json = await _api.get('/orders/history');
      history = (json as List).map((o) => DeliveryOrder.fromJson(o)).toList();
      notifyListeners();
    } catch (e) {
      errorMessage = e.toString();
      notifyListeners();
    }
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    _routeTimer?.cancel();
    super.dispose();
  }
}
