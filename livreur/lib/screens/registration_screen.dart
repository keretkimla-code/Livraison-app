import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/courier.dart';
import '../state/app_state.dart';
import '../models/order.dart';
import 'home_screen.dart';

enum _Step { phone, otp, profile, pending }

class RegistrationScreen extends StatefulWidget {
  const RegistrationScreen({super.key});

  @override
  State<RegistrationScreen> createState() => _RegistrationScreenState();
}

class _RegistrationScreenState extends State<RegistrationScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _otpController = TextEditingController();
  final _plateController = TextEditingController();
  VehicleType _vehicleType = VehicleType.moto;
  _Step _step = _Step.phone;

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    _otpController.dispose();
    _plateController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final profile = appState.profile;

    if (profile.status == RegistrationStatus.validated) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (_) => const HomeScreen()),
        );
      });
    }
        if (profile.status == RegistrationStatus.pending && _step == _Step.profile) {
      return Scaffold(
        appBar: AppBar(title: const Text('Inscription Livreur')),
        body: const Center(
          child: Padding(
            padding: EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.hourglass_top, size: 64, color: Colors.orange),
                SizedBox(height: 16),
                Text(
                  'Dossier envoyé avec succès !',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  textAlign: TextAlign.center,
                ),
                SizedBox(height: 8),
                Text(
                  'Un administrateur va vérifier tes documents. Reviens bientôt pour voir si ton compte est validé.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.black54),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Inscription Livreur')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'Livraison Livreur — bêta',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 24),
            if (appState.errorMessage != null) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.red.withOpacity(0.08),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(appState.errorMessage!, style: const TextStyle(color: Colors.red)),
              ),
              const SizedBox(height: 16),
            ],
            if (_step == _Step.phone) _buildPhoneStep(appState),
            if (_step == _Step.otp) _buildOtpStep(appState),
            if (_step == _Step.profile) _buildProfileStep(appState),
          ],
        ),
      ),
    );
  }

  Widget _buildPhoneStep(AppState appState) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Connecte-toi avec ton numéro de téléphone.',
          style: TextStyle(fontSize: 13, color: Colors.black54),
        ),
        const SizedBox(height: 16),
        TextFormField(
          controller: _phoneController,
          keyboardType: TextInputType.phone,
          decoration: const InputDecoration(
            labelText: 'Numéro de téléphone',
            hintText: '+235 66 00 00 00',
            border: OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 16),
        FilledButton(
          onPressed: appState.isBusy
              ? null
              : () async {
                  appState.updatePhoneNumber(_phoneController.text);
                  final ok = await appState.sendOtp();
                  if (ok) setState(() => _step = _Step.otp);
                },
          child: appState.isBusy
              ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2))
              : const Text('Recevoir le code OTP'),
        ),
      ],
    );
  }

  Widget _buildOtpStep(AppState appState) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text('Un code a été envoyé au ${appState.phoneNumber}'),
        const SizedBox(height: 4),
        const Text(
          '(Mode bêta : utilise 0000 comme code universel de test)',
          style: TextStyle(fontSize: 12, color: Colors.black54),
        ),
        const SizedBox(height: 16),
        TextFormField(
          controller: _nameController,
          decoration: const InputDecoration(
            labelText: 'Nom complet',
            border: OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 16),
        TextFormField(
          controller: _otpController,
          keyboardType: TextInputType.number,
          maxLength: 4,
          decoration: const InputDecoration(
            labelText: 'Code OTP',
            border: OutlineInputBorder(),
          ),
        ),
        FilledButton(
          onPressed: appState.isBusy
              ? null
              : () async {
                  final ok = await appState.verifyOtp(_otpController.text, _nameController.text);
                  if (ok) setState(() => _step = _Step.profile);
                },
          child: appState.isBusy
              ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2))
              : const Text('Vérifier'),
        ),
      ],
    );
  }

  Widget _buildProfileStep(AppState appState) {
    final profile = appState.profile;

    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Renseigne ton véhicule pour finaliser ton dossier.',
            style: TextStyle(fontSize: 13, color: Colors.black54),
          ),
          const SizedBox(height: 16),
          DropdownButtonFormField<VehicleType>(
            value: _vehicleType,
            decoration: const InputDecoration(
              labelText: 'Type de véhicule',
              border: OutlineInputBorder(),
            ),
            items: VehicleType.values
                .map((v) => DropdownMenuItem(value: v, child: Text(v.label)))
                .toList(),
            onChanged: (v) {
              if (v != null) {
                setState(() => _vehicleType = v);
                appState.updateVehicleType(v);
              }
            },
          ),
          const SizedBox(height: 16),
          TextFormField(
            controller: _plateController,
            decoration: const InputDecoration(
              labelText: "Numéro d'immatriculation",
              border: OutlineInputBorder(),
            ),
            onChanged: appState.updatePlateNumber,
            validator: (v) => (v == null || v.isEmpty) ? 'Champ requis' : null,
          ),
          const SizedBox(height: 24),
          _UploadTile(
            title: "Pièce d'identité (CNI, permis...)",
            uploaded: profile.idUploaded,
            onTap: appState.markIdUploaded,
          ),
          const SizedBox(height: 12),
          _UploadTile(
            title: 'Photo du véhicule + carte grise',
            uploaded: profile.vehiclePhotoUploaded,
            onTap: appState.markVehiclePhotoUploaded,
          ),
          const SizedBox(height: 28),
          FilledButton(
            onPressed: (!appState.isBusy && profile.idUploaded && profile.vehiclePhotoUploaded)
                ? () async {
                    if (_formKey.currentState!.validate()) {
                      await appState.submitRegistration();
                    }
                  }
                : null,
            style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16)),
            child: appState.isBusy
                ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text('Envoyer mon dossier pour validation'),
          ),
        ],
      ),
    );
  }
}

class _UploadTile extends StatelessWidget {
  final String title;
  final bool uploaded;
  final VoidCallback onTap;

  const _UploadTile({
    required this.title,
    required this.uploaded,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: uploaded ? null : onTap,
      borderRadius: BorderRadius.circular(10),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          border: Border.all(color: uploaded ? Colors.green : Colors.grey),
          borderRadius: BorderRadius.circular(10),
          color: uploaded ? Colors.green.withOpacity(0.08) : null,
        ),
        child: Row(
          children: [
            Icon(
              uploaded ? Icons.check_circle : Icons.upload_file,
              color: uploaded ? Colors.green : Colors.grey.shade700,
            ),
            const SizedBox(width: 12),
            Expanded(child: Text(title)),
            if (!uploaded) const Text('Ajouter', style: TextStyle(fontSize: 12)),
          ],
        ),
      ),
    );
  }
}
