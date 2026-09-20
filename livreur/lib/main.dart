import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'screens/registration_screen.dart';
import 'screens/home_screen.dart';
import 'screens/navigation_screen.dart';
import 'state/app_state.dart';

void main() {
  runApp(const LivraisonCourierApp());
}

class LivraisonCourierApp extends StatelessWidget {
  const LivraisonCourierApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => AppState(),
      child: MaterialApp(
        title: 'Livraison Livreur',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          colorSchemeSeed: const Color(0xFFE65100),
          useMaterial3: true,
        ),
        home: const _RootScreen(),
      ),
    );
  }
}

class _RootScreen extends StatelessWidget {
  const _RootScreen();

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();

    if (appState.isRestoring) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    if (appState.isAuthenticated && appState.currentOrder != null) {
      return const NavigationScreen();
    }
    if (appState.isAuthenticated && appState.profile.status.name == 'validated') {
      return const HomeScreen();
    }
    return const RegistrationScreen();
  }
}