import 'dart:convert';
import 'package:http/http.dart' as http;

/// Exception levée pour toute erreur renvoyée par l'API (statut 4xx/5xx),
/// avec le message "detail" renvoyé par FastAPI quand disponible.
class ApiException implements Exception {
  final String message;
  ApiException(this.message);

  @override
  String toString() => message;
}

/// Client HTTP minimal pour parler au backend FastAPI.
///
/// ⚠️ Adapte [baseUrl] selon où tu testes :
/// - Émulateur Android : `http://10.0.2.2:8000` (10.0.2.2 pointe vers le
///   localhost de la machine hôte depuis l'émulateur)
/// - Simulateur iOS : `http://127.0.0.1:8000`
/// - Téléphone physique : l'adresse IP locale de ton ordinateur sur le
///   même réseau Wi-Fi (ex. `http://192.168.1.42:8000`), en lançant le
///   backend avec `uvicorn app.main:app --host 0.0.0.0`
/// - Backend déployé en ligne : son URL publique en HTTPS
class ApiClient {
    static const String baseUrl = 'https://scaling-goldfish-wrw75w4pxvp6f9vpp-8000.app.github.dev';

  String? _token;

  void setToken(String token) => _token = token;
  void clearToken() => _token = null;

  Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        if (_token != null) 'Authorization': 'Bearer $_token',
      };

  dynamic _handle(http.Response response) {
    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) return null;
      return jsonDecode(utf8.decode(response.bodyBytes));
    }
    String detail = 'Erreur réseau (${response.statusCode})';
    try {
      final decoded = jsonDecode(utf8.decode(response.bodyBytes));
      if (decoded is Map && decoded['detail'] != null) {
        detail = decoded['detail'].toString();
      }
    } catch (_) {
      // corps de réponse non JSON, on garde le message par défaut
    }
    throw ApiException(detail);
  }

  Future<dynamic> get(String path) async {
    final res = await http
        .get(Uri.parse('$baseUrl$path'), headers: _headers)
        .timeout(const Duration(seconds: 10));
    return _handle(res);
  }

  Future<dynamic> post(String path, [Map<String, dynamic>? body]) async {
    final res = await http
        .post(
          Uri.parse('$baseUrl$path'),
          headers: _headers,
          body: body != null ? jsonEncode(body) : null,
        )
        .timeout(const Duration(seconds: 10));
    return _handle(res);
  }

  Future<dynamic> patch(String path, [Map<String, dynamic>? body]) async {
    final res = await http
        .patch(
          Uri.parse('$baseUrl$path'),
          headers: _headers,
          body: body != null ? jsonEncode(body) : null,
        )
        .timeout(const Duration(seconds: 10));
    return _handle(res);
  }
}
