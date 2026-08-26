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

  @override
  void dispose() {
    _codeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.read<AppState>();
    final order = context.watch<AppState>().currentOrder;

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
                      final ok = await appState.confirmDelivery(code: _codeController.text);
                      if (ok && context.mounted) {
                        _goHome(context);
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
