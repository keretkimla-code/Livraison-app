import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/order.dart';
import '../state/app_state.dart';
import 'delivery_confirmation_screen.dart';

class NavigationScreen extends StatelessWidget {
  const NavigationScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final order = appState.currentOrder;

    if (order == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Course')),
        body: const Center(child: Text('Aucune course en cours.')),
      );
    }

    final headingToPickup = order.status == OrderStatus.headingToPickup ||
        order.status == OrderStatus.atPickup;

    return Scaffold(
      appBar: AppBar(
        title: Text(headingToPickup ? 'Direction : collecte' : 'Direction : livraison'),
      ),
      body: Column(
        children: [
          Expanded(
            child: Stack(
              children: [
                CustomPaint(
                  size: Size.infinite,
                  painter: _RoutePainter(progress: order.routeProgress),
                ),
                const Positioned(
                  bottom: 8,
                  left: 0,
                  right: 0,
                  child: Text(
                    'Carte simplifiée (bêta) — intégration Google Maps / OSM prévue en V1',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 11, color: Colors.black54),
                  ),
                ),
              ],
            ),
          ),
          Card(
            margin: const EdgeInsets.all(16),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    order.status.label,
                    style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 4),
                  Text(headingToPickup ? order.pickupAddress : order.dropoffAddress),
                  const SizedBox(height: 4),
                  Text('Commande ${order.id} · ${order.price} FCFA'),
                  const SizedBox(height: 16),
                  if (appState.errorMessage != null) ...[
                    Text(appState.errorMessage!, style: const TextStyle(color: Colors.red, fontSize: 12)),
                    const SizedBox(height: 8),
                  ],
                  if (order.status == OrderStatus.atPickup)
                    FilledButton(
                      onPressed: appState.isBusy ? null : () => context.read<AppState>().confirmPickup(),
                      style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(48)),
                      child: appState.isBusy
                          ? const SizedBox(
                              height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2))
                          : const Text('Colis récupéré — direction le client'),
                    ),
                  if (order.status == OrderStatus.atDropoff)
                    FilledButton(
                      onPressed: () {
                        Navigator.of(context).pushReplacement(
                          MaterialPageRoute(builder: (_) => const DeliveryConfirmationScreen()),
                        );
                      },
                      style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(48)),
                      child: const Text('Confirmer la livraison'),
                    ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _RoutePainter extends CustomPainter {
  final double progress;

  _RoutePainter({required this.progress});

  @override
  void paint(Canvas canvas, Size size) {
    final start = Offset(size.width * 0.15, size.height * 0.85);
    final end = Offset(size.width * 0.85, size.height * 0.15);

    final linePaint = Paint()
      ..color = Colors.grey.shade400
      ..strokeWidth = 4
      ..style = PaintingStyle.stroke;

    _drawDashedLine(canvas, start, end, linePaint);

    canvas.drawCircle(start, 9, Paint()..color = Colors.green.shade700);
    canvas.drawCircle(end, 9, Paint()..color = Colors.red.shade700);

    final clamped = progress < 0 ? 0.0 : (progress > 1 ? 1.0 : progress);
    final courierPos = Offset(
      start.dx + (end.dx - start.dx) * clamped,
      start.dy + (end.dy - start.dy) * clamped,
    );
    canvas.drawCircle(courierPos, 12, Paint()..color = Colors.blue.shade700);
    canvas.drawCircle(courierPos, 5, Paint()..color = Colors.white);
  }

  void _drawDashedLine(Canvas canvas, Offset start, Offset end, Paint paint) {
    const dashWidth = 8.0;
    const dashSpace = 6.0;
    final total = (end - start).distance;
    if (total == 0) return;
    final direction = Offset((end.dx - start.dx) / total, (end.dy - start.dy) / total);
    double drawn = 0;
    while (drawn < total) {
      final segStartDist = drawn;
      final segEndDist = (drawn + dashWidth) < total ? (drawn + dashWidth) : total;
      final segStart = Offset(
        start.dx + direction.dx * segStartDist,
        start.dy + direction.dy * segStartDist,
      );
      final segEnd = Offset(
        start.dx + direction.dx * segEndDist,
        start.dy + direction.dy * segEndDist,
      );
      canvas.drawLine(segStart, segEnd, paint);
      drawn += dashWidth + dashSpace;
    }
  }

  @override
  bool shouldRepaint(covariant _RoutePainter oldDelegate) => oldDelegate.progress != progress;
}
