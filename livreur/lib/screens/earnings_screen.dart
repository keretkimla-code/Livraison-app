import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../state/app_state.dart';
import '../models/order.dart';

class EarningsScreen extends StatefulWidget {
  const EarningsScreen({super.key});

  @override
  State<EarningsScreen> createState() => _EarningsScreenState();
}

class _EarningsScreenState extends State<EarningsScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AppState>().refreshProfile();
      context.read<AppState>().loadHistory();
    });
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final completedCount = appState.history.length;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Mes gains'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              context.read<AppState>().refreshProfile();
              context.read<AppState>().loadHistory();
            },
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Card(
              color: Theme.of(context).colorScheme.primaryContainer,
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  children: [
                    const Text('Total des gains'),
                    const SizedBox(height: 8),
                    Text(
                      '${appState.profile.totalEarnings} FCFA',
                      style: const TextStyle(fontSize: 32, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            _StatTile(label: 'Courses effectuées', value: '$completedCount'),
            _StatTile(
              label: 'Note moyenne',
              value: '${appState.profile.ratingAvg.toStringAsFixed(1)} / 5 (${appState.profile.ratingCount} avis)',
            ),
            const SizedBox(height: 20),
            const Text(
              "Un gain n'est comptabilisé qu'une fois le client connecté et le paiement "
              "validé de son côté. Le retrait des gains (Mobile Money) sera disponible "
              "en version V1.",
              style: TextStyle(fontSize: 12, color: Colors.black54),
            ),
          ],
        ),
      ),
    );
  }
}

class _StatTile extends StatelessWidget {
  final String label;
  final String value;

  const _StatTile({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        title: Text(label),
        trailing: Text(value, style: const TextStyle(fontWeight: FontWeight.bold)),
      ),
    );
  }
}
