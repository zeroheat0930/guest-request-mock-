import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiClient {
  static const String baseUrl = 'http://localhost:3000/api/guest';

  Future<Map<String, dynamic>> _get(String path, [Map<String, String>? query]) async {
    final uri = Uri.parse('$baseUrl$path').replace(queryParameters: query);
    final res = await http.get(uri);
    return jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> _post(String path, Map<String, dynamic> body) async {
    final uri = Uri.parse('$baseUrl$path');
    final res = await http.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(body),
    );
    return jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
  }

  // 어메니티 품목 마스터
  Future<Map<String, dynamic>> fetchAmenityItems() => _get('/amenity/items');

  // 어메니티 요청 등록
  Future<Map<String, dynamic>> requestAmenity({
    required String rsvNo,
    required String roomNo,
    required List<Map<String, dynamic>> itemList,
    String? reqMemo,
  }) =>
      _post('/amenity', {
        'rsvNo': rsvNo,
        'roomNo': roomNo,
        'itemList': itemList,
        'reqMemo': reqMemo ?? '',
      });

  // 하우스키핑 상태 조회
  Future<Map<String, dynamic>> fetchHousekeeping(String rsvNo) =>
      _get('/housekeeping', {'rsvNo': rsvNo});

  // 하우스키핑 상태 변경 (MU/DND/CLR)
  Future<Map<String, dynamic>> updateHousekeeping({
    required String rsvNo,
    required String hkStatCd,
    String? reqMemo,
  }) =>
      _post('/housekeeping', {
        'rsvNo': rsvNo,
        'hkStatCd': hkStatCd,
        'reqMemo': reqMemo ?? '',
      });

  // 레이트 체크아웃 조회
  Future<Map<String, dynamic>> checkLateCheckout({
    required String rsvNo,
    required String reqOutTm,
  }) =>
      _get('/late-checkout', {'rsvNo': rsvNo, 'reqOutTm': reqOutTm});

  // 레이트 체크아웃 신청
  Future<Map<String, dynamic>> requestLateCheckout({
    required String rsvNo,
    required String reqOutTm,
    required int addAmt,
  }) =>
      _post('/late-checkout', {
        'rsvNo': rsvNo,
        'reqOutTm': reqOutTm,
        'addAmt': addAmt,
      });
}
