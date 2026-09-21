import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../state/app_state.dart';
import 'home_screen.dart';

class DeliveryConfirmationScreen extends StatefulWidget {
  const DeliveryConfirmationScreen({super.key});

  @override
  State<DeliveryConfirmationScreen> createState() => _DeliveryConfirmationScreenState();
}

class _DeliveryConfirmationScreenState extends State<DeliveryConfirmationScreen> {
  final _codeController = TextEditingController();
  String? _error;
  bool _waitingForPayment = false;
  bool _paymentReceived = false;
  String? _deliveredOrderId;
  Timer? _paymentCheckTimer;

  @override
  void dispose() {
    _codeController.dispose();
    _paymentCheckTimer?.cancel();
    super.dispose();
  }

  void _startWatchingPayment(String orderId) {
    _deliveredOrderId = orderId;
    _paymentCheckTimer?.cancel();
    _paymentCheckTimer = Timer.periodic(const Duration(seconds: 5), (_) async {
      final appState = context.read<AppState>();
      final data = await appState.checkOrderStatus(orderId);
      if (data != null && data['status'] == 'paid' && mounted) {
        setState(() => _paymentReceived = true);
        _paymentCheckTimer?.cancel();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.read<AppState>();
    final order = context.watch<AppState>().currentOrder;

    if (_waitingForPayment) {
      return Scaffold(
        appBar: AppBar(title: const Text('Livraison confirmée')),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: _paymentReceived
                  ? [
                const Icon(Icons.check_circle, size: 64, color: Colors.green),
                const SizedBox(height: 16),
                const Text(
                  'Paiement reçu !',
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),
                const Text(
                  'Ton solde a été crédité. Consulte l\'onglet Gains pour le détail.',
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: () => _goHome(context),
                  child: const Text('Retour à l\'accueil'),
                ),
              ]
                  : [
                const SizedBox(
                  width: 40,
                  height: 40,
                  child: CircularProgressIndicator(strokeWidth: 3),
                ),
                const SizedBox(height: 16),
                const Text(
                  'Livraison confirmée, en attente du paiement du client...',
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 24),
                TextButton(
                  onPressed: () => _goHome(context),
                  child: const Text('Retourner à l\'accueil sans attendre'),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Confirmer la livraison')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text('Demande au client le code affiché sur son application.'),
            const SizedBox(height: 8),
            if (order?.deliveryCode != null)
              Text(
                '(Mode bêta — puisque l\'app Client n\'est pas encore connectée, '
                    'voici le code généré par le serveur pour cette commande : ${order!.deliveryCode})',
                style: const TextStyle(fontSize: 12, color: Colors.black54),
              ),
            const SizedBox(height: 16),
            TextField(
              controller: _codeController,
              keyboardType: TextInputType.number,
              maxLength: 4,
              decoration: const InputDecoration(
                labelText: 'Code de confirmation',
                border: OutlineInputBorder(),
              ),
            ),
            if (_error != null) ...[
              Text(_error!, style: const TextStyle(color: Colors.red)),
              const SizedBox(height: 8),
            ],
            const SizedBox(height: 12),
            FilledButton(
              onPressed: appState.isBusy
                  ? null
                  : () async {
                final orderId = order?.id;
                final ok = await appState.confirmDelivery(code: _codeController.text);
                if (ok && context.mounted && orderId != null) {
                  setState(() => _waitingForPayment = true);
                  _startWatchingPayment(orderId);
                } else {
                  setState(() => _error = appState.errorMessage ?? 'Code incorrect.');
                }
              },
              child: appState.isBusy
                  ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Text('Valider la livraison'),
            ),
          ],
        ),
      ),
    );
  }

  void _goHome(BuildContext context) {
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => const HomeScreen()),
          (route) => false,
    );
  }
}