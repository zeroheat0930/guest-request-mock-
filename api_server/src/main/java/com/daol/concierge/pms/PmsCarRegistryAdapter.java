package com.daol.concierge.pms;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PMS `PMS_CAR_NO` 테이블 차량 등록 어댑터.
 *
 * `/api/ko/client/registerConciergeCar` (사내 PMS 에 컨시어지용으로 추가한 엔드포인트) 를 호출해
 * 기존 프론트데스크 차량 관리 모달에 우리 컨시어지 등록 차량이 그대로 나타나게 한다.
 *
 * concierge.dispatcher=daol 일 때만 활성화. 실패는 로그만 남기고 삼킨다 —
 * 게스트 접수 성공/실패와 무관하게 설계 (KOK_EVENT 와 동일 정책).
 */
@Component
@ConditionalOnProperty(name = "concierge.dispatcher", havingValue = "daol")
public class PmsCarRegistryAdapter {

	private static final Logger log = LoggerFactory.getLogger(PmsCarRegistryAdapter.class);

	@Value("${pms.base-url:http://localhost:8090}")
	private String baseUrl;

	@Value("${pms.prop-cd:}")
	private String propCd;

	@Value("${pms.cmpx-cd:}")
	private String cmpxCd;

	private final HttpClient http = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(3))
			.build();

	private final ObjectMapper mapper = new ObjectMapper();

	public void register(String rsvNo, String carNo) {
		try {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("propCd", propCd);
			body.put("cmpxCd", cmpxCd);
			body.put("refNo", rsvNo);
			body.put("carNo", carNo);

			HttpRequest req = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl + "/api/ko/client/registerConciergeCar"))
					.timeout(Duration.ofSeconds(5))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
					.build();

			HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() >= 400) {
				log.warn("[pms-car] register failed http={} body={}", res.statusCode(), res.body());
				return;
			}
			log.debug("[pms-car] registered rsvNo={} carNo={}", rsvNo, carNo);
		} catch (Exception e) {
			log.warn("[pms-car] register failed: {}", e.getMessage());
		}
	}
}
