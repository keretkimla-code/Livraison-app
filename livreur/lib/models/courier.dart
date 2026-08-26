enum VehicleType { moto, tricycle, vehicule }

extension VehicleTypeLabel on VehicleType {
  String get label {
    switch (this) {
      case VehicleType.moto:
        return 'Moto';
      case VehicleType.tricycle:
        return 'Tricycle';
      case VehicleType.vehicule:
        return 'Véhicule';
    }
  }

  String get apiValue => name; // 'moto' | 'tricycle' | 'vehicule' — identique côté API

  static VehicleType fromApi(String value) {
    return VehicleType.values.firstWhere(
      (v) => v.apiValue == value,
      orElse: () => VehicleType.moto,
    );
  }
}

enum RegistrationStatus { notSubmitted, pending, validated, rejected }

extension RegistrationStatusLabel on RegistrationStatus {
  static RegistrationStatus fromApi(String value) {
    switch (value) {
      case 'pending':
        return RegistrationStatus.pending;
      case 'validated':
        return RegistrationStatus.validated;
      case 'rejected':
        return RegistrationStatus.rejected;
      default:
        return RegistrationStatus.notSubmitted;
    }
  }
}

class CourierProfile {
  final String fullName;
  final String phoneNumber;
  final VehicleType vehicleType;
  final String plateNumber;
  final bool idUploaded;
  final bool vehiclePhotoUploaded;
  final RegistrationStatus status;
  final double ratingAvg;
  final int ratingCount;
  final int totalEarnings;

  const CourierProfile({
    this.fullName = '',
    this.phoneNumber = '',
    this.vehicleType = VehicleType.moto,
    this.plateNumber = '',
    this.idUploaded = false,
    this.vehiclePhotoUploaded = false,
    this.status = RegistrationStatus.notSubmitted,
    this.ratingAvg = 5.0,
    this.ratingCount = 0,
    this.totalEarnings = 0,
  });

  CourierProfile copyWith({
    String? fullName,
    String? phoneNumber,
    VehicleType? vehicleType,
    String? plateNumber,
    bool? idUploaded,
    bool? vehiclePhotoUploaded,
    RegistrationStatus? status,
    double? ratingAvg,
    int? ratingCount,
    int? totalEarnings,
  }) {
    return CourierProfile(
      fullName: fullName ?? this.fullName,
      phoneNumber: phoneNumber ?? this.phoneNumber,
      vehicleType: vehicleType ?? this.vehicleType,
      plateNumber: plateNumber ?? this.plateNumber,
      idUploaded: idUploaded ?? this.idUploaded,
      vehiclePhotoUploaded: vehiclePhotoUploaded ?? this.vehiclePhotoUploaded,
      status: status ?? this.status,
      ratingAvg: ratingAvg ?? this.ratingAvg,
      ratingCount: ratingCount ?? this.ratingCount,
      totalEarnings: totalEarnings ?? this.totalEarnings,
    );
  }

  factory CourierProfile.fromJson(Map<String, dynamic> json, {String? fullName, String? phoneNumber}) {
    return CourierProfile(
      fullName: fullName ?? '',
      phoneNumber: phoneNumber ?? '',
      vehicleType: VehicleTypeLabel.fromApi(json['vehicle_type'] as String),
      plateNumber: json['plate_number'] as String? ?? '',
      idUploaded: json['id_document_uploaded'] as bool? ?? false,
      vehiclePhotoUploaded: json['vehicle_photo_uploaded'] as bool? ?? false,
      status: RegistrationStatusLabel.fromApi(json['status'] as String),
      ratingAvg: (json['rating_avg'] as num?)?.toDouble() ?? 5.0,
      ratingCount: (json['rating_count'] as num?)?.toInt() ?? 0,
      totalEarnings: (json['total_earnings'] as num?)?.toInt() ?? 0,
    );
  }
}
