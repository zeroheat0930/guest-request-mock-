package com.daol.concierge.nearby;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 하드코딩 샘플 기반 Mock 주변 정보 제공자 (기본 프로바이더).
 *
 * 데모/개발 환경에서 Kakao API 키 없이도 "NEARBY" 탭이 동작하도록 한다.
 * 데이터는 서울 시청 인근을 상정한 실제 지명으로 구성 — 데모 신뢰도 확보.
 */
@Service
@ConditionalOnProperty(name = "nearby.provider", havingValue = "mock", matchIfMissing = true)
public class MockNearbyProvider implements NearbyProvider {

	private static final Map<String, String> CATEGORY_NM = Map.of(
			"food",     "음식점",
			"cafe",     "카페",
			"conv",     "편의점",
			"tour",     "관광지",
			"pharmacy", "약국"
	);

	private static final Map<String, List<Object[]>> SAMPLES = Map.of(
			"food", List.of(
					// {name, distanceM, phone, address}
					new Object[]{"무교동 북어국집",       120, "02-739-6737", "서울 중구 을지로1길 38"},
					new Object[]{"미진 무교점 메밀소바", 240, "02-755-0301", "서울 중구 세종대로 19길 16"},
					new Object[]{"한정식 목란",           480, "02-734-6654", "서울 중구 정동길 21"},
					new Object[]{"광장시장 육회골목",     780, "02-2267-0291", "서울 종로구 창경궁로 88"},
					new Object[]{"을지면옥",            1100, "02-2266-7052", "서울 중구 충무로14길 2-1"}
			),
			"cafe", List.of(
					new Object[]{"프릳츠커피 원서점",   180, "02-747-8101", "서울 종로구 율곡로3길 74-8"},
					new Object[]{"스타벅스 시청점",     260, "1522-3232",   "서울 중구 세종대로 136"},
					new Object[]{"테라로사 광화문점",   410, "02-6262-5290", "서울 종로구 종로 1"},
					new Object[]{"블루보틀 삼청점",     920, "02-3445-0550", "서울 종로구 삼청로 11"}
			),
			"conv", List.of(
					new Object[]{"GS25 서울시청점",      80,  "02-755-8410", "서울 중구 세종대로 110"},
					new Object[]{"CU 덕수궁점",         220, "02-773-2212", "서울 중구 세종대로 99"},
					new Object[]{"세븐일레븐 광화문점", 390, "02-720-1147", "서울 종로구 세종대로 172"},
					new Object[]{"이마트24 을지로점",    560, "02-2266-8820", "서울 중구 을지로 16"}
			),
			"tour", List.of(
					new Object[]{"덕수궁",        180, null, "서울 중구 세종대로 99"},
					new Object[]{"서울시청 광장", 60,  null, "서울 중구 세종대로 110"},
					new Object[]{"광화문",        520, null, "서울 종로구 사직로 161"},
					new Object[]{"청계천 광장",   430, null, "서울 종로구 창경궁로 136"},
					new Object[]{"명동성당",      950, null, "서울 중구 명동길 74"}
			),
			"pharmacy", List.of(
					new Object[]{"온누리약국 시청점",   150, null, "서울 중구 무교로 6"},
					new Object[]{"종로약국",            470, null, "서울 종로구 종로 19"},
					new Object[]{"광화문약국",          610, null, "서울 종로구 새문안로 92"},
					new Object[]{"을지로약국",          820, null, "서울 중구 을지로 29"}
			)
	);

	@Override
	public List<NearbyPlace> search(double lat, double lng, String categoryCd, int radiusM) {
		List<Object[]> rows = SAMPLES.get(categoryCd);
		if (rows == null) return List.of();
		String categoryNm = CATEGORY_NM.getOrDefault(categoryCd, categoryCd);
		return rows.stream()
				.map(r -> new NearbyPlace(
						(String) r[0],
						categoryNm,
						categoryCd,
						(Integer) r[1],
						(String) r[2],
						(String) r[3],
						buildMapUrl((String) r[0]),
						null
				))
				.sorted(Comparator.comparingInt(NearbyPlace::distanceM))
				.toList();
	}

	private static String buildMapUrl(String name) {
		return "https://map.kakao.com/link/search/" + URLEncoder.encode(name, StandardCharsets.UTF_8);
	}
}
