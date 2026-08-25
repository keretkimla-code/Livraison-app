import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/order.dart';
import '../state/app_state.dart';
import 'earnings_screen.dart';
import 'history_screen.dart';
import 'navigation_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _tabIndex = 0;

  @override
  Widget build(BuildContext context) {
    final pages = const [
      _RequestsTab(),
      EarningsScreen(),
      HistoryScreen(),
    ];

    return Scaffold(
      body: pages[_tabIndex],
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tabIndex,
        onDestinationSelected: (i) => setState(() => _tabIndex = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.local_shipping), label: 'Courses'),
          NavigationDestination(icon: Icon(Icons.payments), label: 'Gains'),
          NavigationDestination(icon: Icon(Icons.history), label: 'Historique'),
        ],
      ),
    );
  }
}

class _RequestsTab extends StatelessWidget {
  const _RequestsTab();

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();

    return Scaffold(
      appBar: AppBar(
        title: Text(
          'Bonjour, ${appState.profile.fullName.isEmpty ? "Livreur" : appState.profile.fullName.split(' ').first}',
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: Row(
              children: [
                Text(appState.isAvailable ? 'Disponible' : 'Indisponible'),
                Switch(
                  value: appState.isAvailable,
                  onChanged: appState.isBusy
                      ? null
                      : (v) => context.read<AppState>().setAvailable(v),
                ),
              ],
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          if (appState.errorMessage != null)
            Container(
              width: double.infinity,
              color: Colors.red.withOpacity(0.08),
              padding: const EdgeInsets.all(10),
              child: Text(appState.errorMessage!, style: const TextStyle(color: Colors.red, fontSize: 12)),
            ),
          Expanded(
            child: !appState.isAvailable
                ? const _OfflineState()
                : appState.incomingRequests.isEmpty
                    ? const _WaitingState()
                    : ListView.builder(
                        padding: const EdgeInsets.all(12),
                        itemCount: appState.incomingRequests.length,
                        itemBuilder: (context, index) {
                          final order = appState.incomingRequests[index];
                          return _RequestCard(order: order);
                        },
                      ),
          ),
        ],
      ),
    );
  }
}

class _OfflineState extends StatelessWidget {
  const _OfflineState();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.power_settings_new, size: 56, color: Colors.grey),
            const SizedBox(height: 12),
            const Text(
              'Tu es hors ligne',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 4),
            const Text(
              'Active ton statut "Disponible" pour recevoir des demandes de livraison à proximité.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.black54),
            ),
          ],
        ),
      ),
    );
  }
}

class _WaitingState extends StatelessWidget {
  const _WaitingState();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Padding(
        padding: EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox(
              width: 40,
              height: 40,
              child: CircularProgressIndicator(strokeWidth: 3),
            ),
            SizedBox(height: 16),
            Text(
              'En attente de demandes à proximité...',
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}

class _RequestCard extends StatelessWidget {
  final DeliveryOrder order;

  const _RequestCard({required this.order});

  @override
  Widget build(BuildContext context) {
    final appState = context.read<AppState>();

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(order.id, style: const TextStyle(fontWeight: FontWeight.bold)),
                Chip(label: Text('${order.distanceKm.toStringAsFixed(1)} km')),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Icon(Icons.circle, size: 10, color: Colors.green),
                const SizedBox(width: 6),
                Expanded(child: Text(order.pickupAddress)),
              ],
            ),
            const SizedBox(height: 4),
            Row(
              children: [
                const Icon(Icons.location_on, size: 14, color: Colors.red),
                const SizedBox(width: 6),
                Expanded(child: Text(order.dropoffAddress)),
              ],
            ),
            const SizedBox(height: 8),
            Text('Colis : ${order.parcelType.label}'),
            const SizedBox(height: 8),
            Text(
              '${order.price} FCFA',
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => appState.declineRequest(order.id),
                    child: const Text('Refuser'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton(
                    onPressed: () async {
                      final ok = await appState.acceptRequest(order.id);
                      if (ok && context.mounted) {
                        Navigator.of(context).push(
                          MaterialPageRoute(builder: (_) => const NavigationScreen()),
                        );
                      }
                    },
                    child: const Text('Accepter'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
