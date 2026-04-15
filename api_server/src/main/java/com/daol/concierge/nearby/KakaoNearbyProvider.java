package com.daol.concierge.nearby;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kakao Local API 기반 주변 정보 제공자.
 *
 * `nearby.provider=kakao` 일 때만 활성. 키 미설정/실패 시 빈 리스트를 반환해
 * 장애가 게스트 경험을 깨뜨리지 않도록 한다.
 */
@Service
@ConditionalOnProperty(name = "nearby.provider", havingValue = "kakao")
public class KakaoNearbyProvider implements NearbyProvider {

	private static final Logger log = LoggerFactory.getLogger(KakaoNearbyProvider.class);

	private static final String ENDPOINT = "https://dapi.kakao.com/v2/local/search/category.json";

	private static final Map<String, String> GROUP_CODE = Map.of(
			"food",     "FD6",
			"cafe",     "CE7",
			"conv",     "CS2",
			"tour",     "AT4",
			"pharmacy", "PM9"
	);

	private static final Map<String, String> CATEGORY_NM = Map.of(
			"food",     "음식점",
			"cafe",     "카페",
			"conv",     "편의점",
			"tour",     "관광지",
			"pharmacy", "약국"
	);

	@Value("${kakao.rest-api-key:}")
	private String apiKey;

	private final HttpClient http = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public List<NearbyPlace> search(double lat, double lng, String categoryCd, int radiusM) {
		if (apiKey == null || apiKey.isBlank()) {
			log.warn("[nearby/kakao] KAKAO_REST_API_KEY not set — returning empty list");
			return List.of();
		}
		String groupCode = GROUP_CODE.get(categoryCd);
		if (groupCode == null) return List.of();

		String url = ENDPOINT
				+ "?category_group_code=" + groupCode
				+ "&x=" + lng
				+ "&y=" + lat
				+ "&radius=" + radiusM
				+ "&sort=distance"
				+ "&size=15";
		try {
			HttpRequest req = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.timeout(Duration.ofSeconds(5))
					.header("Authorization", "KakaoAK " + apiKey)
					.GET()
					.build();
			HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() != 200) {
				log.warn("[nearby/kakao] http={} body={}", res.statusCode(), res.body());
				return List.of();
			}
			JsonNode root = mapper.readTree(res.body());
			JsonNode docs = root.path("documents");
			String categoryNm = CATEGORY_NM.getOrDefault(categoryCd, categoryCd);
			List<NearbyPlace> out = new ArrayList<>();
			for (JsonNode d : docs) {
				String name = d.path("place_name").asText("");
				String categoryFull = d.path("category_name").asText("");
				String displayCat = categoryFull;
				if (categoryFull.contains(" > ")) {
					String[] parts = categoryFull.split(" > ");
					displayCat = parts[parts.length - 1];
				}
				if (displayCat == null || displayCat.isBlank()) displayCat = categoryNm;
				Integer distanceM = null;
				String distStr = d.path("distance").asText("");
				if (!distStr.isBlank()) {
					try { distanceM = Integer.parseInt(distStr); } catch (NumberFormatException ignore) {}
				}
				String phone = nullIfBlank(d.path("phone").asText(""));
				String address = nullIfBlank(d.path("road_address_name").asText(""));
				if (address == null) address = nullIfBlank(d.path("address_name").asText(""));
				String mapUrl = nullIfBlank(d.path("place_url").asText(""));
				out.add(new NearbyPlace(name, displayCat, categoryCd, distanceM, phone, address, mapUrl, null));
			}
			return out;
		} catch (Exception e) {
			log.warn("[nearby/kakao] call failed: {}", e.getMessage());
			return List.of();
		}
	}

	private static String nullIfBlank(String s) {
		return (s == null || s.isBlank()) ? null : s;
	}
}
