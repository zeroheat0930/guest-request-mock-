import 'package:flutter/material.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final menus = [
      {
        'title': '어메니티 요청',
        'subtitle': 'Amenity Request',
        'icon': Icons.room_service,
      },
      {
        'title': '객실 청소',
        'subtitle': 'Make-up / DND',
        'icon': Icons.cleaning_services,
      },
      {
        'title': '레이트 체크아웃',
        'subtitle': 'Late Checkout',
        'icon': Icons.schedule,
      },
    ];

    return Scaffold(
      appBar: AppBar(
        title: const Text('Guest Service'),
        centerTitle: true,
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: LayoutBuilder(
          builder: (ctx, bc) {
            final cross = bc.maxWidth > 900 ? 3 : (bc.maxWidth > 600 ? 2 : 1);
            return GridView.count(
              crossAxisCount: cross,
              crossAxisSpacing: 16,
              mainAxisSpacing: 16,
              childAspectRatio: 1.3,
              children: menus.map((m) {
                return Card(
                  elevation: 2,
                  child: InkWell(
                    onTap: () {
                      // TODO: 각 기능 화면 연결
                    },
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(m['icon'] as IconData, size: 56, color: Colors.indigo),
                        const SizedBox(height: 12),
                        Text(
                          m['title'] as String,
                          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          m['subtitle'] as String,
                          style: const TextStyle(fontSize: 12, color: Colors.grey),
                        ),
                      ],
                    ),
                  ),
                );
              }).toList(),
            );
          },
        ),
      ),
    );
  }
}
