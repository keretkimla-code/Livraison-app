enum OrderStatus {
  pending,
  accepted,
  headingToPickup,
  atPickup,
  headingToDropoff,
  atDropoff,
  delivered,
  paid,
  cancelled,
}

extension OrderStatusLabel on OrderStatus {
  String get label {
    switch (this) {
      case OrderStatus.pending:
        return 'En attente';
      case OrderStatus.accepted:
        return 'Acceptée';
      case OrderStatus.headingToPickup:
        return 'En route vers la collecte';
      case OrderStatus.atPickup:
        return 'Arrivé au point de collecte';
      case OrderStatus.headingToDropoff:
        return 'En route vers la livraison';
      case OrderStatus.atDropoff:
        return 'Arrivé au point de livraison';
      case OrderStatus.delivered:
        return 'Livrée';
      case OrderStatus.paid:
        return 'Payée';
      case OrderStatus.cancelled:
        return 'Annulée';
    }
  }

  /// Valeur envoyée/reçue de l'API FastAPI (snake_case).
  String get apiValue {
    switch (this) {
      case OrderStatus.pending:
        return 'pending';
      case OrderStatus.accepted:
        return 'accepted';
      case OrderStatus.headingToPickup:
        return 'heading_to_pickup';
      case OrderStatus.atPickup:
        return 'at_pickup';
      case OrderStatus.headingToDropoff:
        return 'heading_to_dropoff';
      case OrderStatus.atDropoff:
        return 'at_dropoff';
      case OrderStatus.delivered:
        return 'delivered';
      case OrderStatus.paid:
        return 'paid';
      case OrderStatus.cancelled:
        return 'cancelled';
    }
  }

  static OrderStatus fromApi(String value) {
    return OrderStatus.values.firstWhere(
      (s) => s.apiValue == value,
      orElse: () => OrderStatus.pending,
    );
  }
}

enum ParcelType { document, colisLeger, colisLourd, repas, courses }

extension ParcelTypeLabel on ParcelType {
  String get label {
    switch (this) {
      case ParcelType.document:
        return 'Document';
      case ParcelType.colisLeger:
        return 'Colis léger';
      case ParcelType.colisLourd:
        return 'Colis lourd';
      case ParcelType.repas:
        return 'Repas';
      case ParcelType.courses:
        return 'Courses';
    }
  }

  String get apiValue {
    switch (this) {
      case ParcelType.document:
        return 'document';
      case ParcelType.colisLeger:
        return 'colis_leger';
      case ParcelType.colisLourd:
        return 'colis_lourd';
      case ParcelType.repas:
        return 'repas';
      case ParcelType.courses:
        return 'courses';
    }
  }

  static ParcelType fromApi(String value) {
    return ParcelType.values.firstWhere(
      (p) => p.apiValue == value,
      orElse: () => ParcelType.colisLeger,
    );
  }
}

class DeliveryOrder {
  final String id;
  final String clientId;
  final String? courierId;
  final String pickupAddress;
  final double pickupLat;
  final double pickupLng;
  final String dropoffAddress;
  final double dropoffLat;
  final double dropoffLng;
  final ParcelType parcelType;
  final int price;
  final double distanceKm;
  final String? paymentMethod;
  final String? deliveryCode;
  OrderStatus status;
  double routeProgress; // 0..1 — animation locale du trajet en cours (bêta)
  int? customerRating;

  DeliveryOrder({
    required this.id,
    required this.clientId,
    this.courierId,
    required this.pickupAddress,
    required this.pickupLat,
    required this.pickupLng,
    required this.dropoffAddress,
    required this.dropoffLat,
    required this.dropoffLng,
    required this.parcelType,
    required this.price,
    required this.distanceKm,
    this.paymentMethod,
    this.deliveryCode,
    this.status = OrderStatus.pending,
    this.routeProgress = 0,
    this.customerRating,
  });

  factory DeliveryOrder.fromJson(Map<String, dynamic> json) {
    return DeliveryOrder(
      id: json['id'] as String,
      clientId: json['client_id'] as String,
      courierId: json['courier_id'] as String?,
      pickupAddress: json['pickup_address'] as String,
      pickupLat: (json['pickup_lat'] as num).toDouble(),
      pickupLng: (json['pickup_lng'] as num).toDouble(),
      dropoffAddress: json['dropoff_address'] as String,
      dropoffLat: (json['dropoff_lat'] as num).toDouble(),
      dropoffLng: (json['dropoff_lng'] as num).toDouble(),
      parcelType: ParcelTypeLabel.fromApi(json['parcel_type'] as String),
      price: (json['price'] as num).toInt(),
      distanceKm: (json['distance_km'] as num).toDouble(),
      paymentMethod: json['payment_method'] as String?,
      deliveryCode: json['delivery_code'] as String?,
      status: OrderStatusLabel.fromApi(json['status'] as String),
    );
  }
}
