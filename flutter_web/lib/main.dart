import 'package:flutter/material.dart';
import 'screens/home_screen.dart';

void main() {
  runApp(const GuestRequestApp());
}

class GuestRequestApp extends StatelessWidget {
  const GuestRequestApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Guest Service',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.indigo),
        useMaterial3: true,
      ),
      home: const HomeScreen(),
    );
  }
}
