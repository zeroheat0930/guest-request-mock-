package com.daol.concierge.dispatcher;

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
 * 회사 PMS(DAOL) 키오스크 이벤트 브릿지 디스패처.
 *
 * 기존 PmsKokEventClient 로직을 그대로 옮겼다. 게스트 요청이 PMS 프론트데스크 화면에
 * 키오스크 이벤트로 실시간 팝업되도록 `/api/mkiosk/event` 로 단일 POST.
 * 실패는 로그만 남기고 삼킨다 — 게스트 접수 성공/실패와 무관하게 설계.
 */
@Component
@ConditionalOnProperty(name = "concierge.dispatcher", havingValue = "daol")
public class DaolKokEventDispatcher implements RequestDispatcher {

	private static final Logger log = LoggerFactory.getLogger(DaolKokEventDispatcher.class);

	@Value("${pms.base-url:http://localhost:8090}")
	private String baseUrl;

	@Value("${pms.prop-cd:}")
	private String propCd;

	@Value("${pms.cmpx-cd:}")
	private String cmpxCd;

	@Value("${pms.kok-cd:CONCIERGE}")
	private String kokCd;

	private final HttpClient http = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(3))
			.build();

	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public void dispatch(RequestEvent event) {
		try {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("propCd", propCd);
			body.put("cmpxCd", cmpxCd);
			body.put("kokCd", kokCd);
			body.put("eventTp", event.eventTp());
			body.put("eventSt", "REQUEST");
			body.put("eventSource", "CONCIERGE");
			body.put("eventTitle", event.eventTitle());
			body.put("userId", "CONCIERGE");

			HttpRequest req = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl + "/api/mkiosk/event"))
					.timeout(Duration.ofSeconds(5))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
					.build();

			HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() >= 400) {
				log.warn("[pms] publish failed http={} body={}", res.statusCode(), res.body());
				return;
			}
			log.debug("[pms] event published tp={} title={}", event.eventTp(), event.eventTitle());
		} catch (Exception e) {
			log.warn("[pms] publish failed ({}): {}", event.eventTp(), e.getMessage());
		}
	}
}
