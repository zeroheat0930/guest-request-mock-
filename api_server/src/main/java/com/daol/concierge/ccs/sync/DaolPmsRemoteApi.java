package com.daol.concierge.ccs.sync;

import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * DAOL PMS 본 시스템 REST API 호출 어댑터.
 *
 * 활성 조건: {@code concierge.pms.api.enabled=true}
 *  + {@code concierge.pms.api.base-url=http://...} 설정.
 *
 * PMS 측 엔드포인트 규약 (사용자가 PMS 본 시스템에 추가할 예정):
 *  - PUT  {base}/api/users/{userId}/use-yn       body: {"useYn":"Y|N"}
 *  - PUT  {base}/api/users/{userId}/dept         body: {"deptCd":"..."}
 *
 * 실패 시 {@link ApiException} 으로 변환되어 어드민 화면에 에러 표시.
 * 인증은 PMS 측 정책에 따라 추가 (현재 X-Internal-Token 헤더로 처리).
 */
@Component
@ConditionalOnProperty(name = "concierge.pms.api.enabled", havingValue = "true")
public class DaolPmsRemoteApi implements PmsRemoteApi {

	private static final Logger log = LoggerFactory.getLogger(DaolPmsRemoteApi.class);

	@Value("${concierge.pms.api.base-url:}")
	private String baseUrl;

	@Value("${concierge.pms.api.token:}")
	private String internalToken;

	private final HttpClient http = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(3))
			.build();

	public DaolPmsRemoteApi() {
		log.info("[pms-api] DaolPmsRemoteApi 활성 — 실 PMS REST 호출 모드.");
	}

	@Override
	public boolean isEnabled() { return true; }

	@Override
	public void updateUserUseYn(String userId, String useYn, String actor) {
		String body = String.format("{\"useYn\":\"%s\",\"actor\":\"%s\"}", esc(useYn), esc(actor));
		put("/api/users/" + enc(userId) + "/use-yn", body);
		log.info("[pms-api] updateUserUseYn userId={} useYn={} by={} OK", userId, useYn, actor);
	}

	@Override
	public void updateUserDept(String userId, String deptCd, String actor) {
		String body = String.format("{\"deptCd\":\"%s\",\"actor\":\"%s\"}", esc(deptCd), esc(actor));
		put("/api/users/" + enc(userId) + "/dept", body);
		log.info("[pms-api] updateUserDept userId={} deptCd={} by={} OK", userId, deptCd, actor);
	}

	private void put(String path, String jsonBody) {
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "PMS API base-url 미설정");
		}
		try {
			HttpRequest.Builder req = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl.replaceAll("/+$", "") + path))
					.timeout(Duration.ofSeconds(5))
					.header("Content-Type", "application/json; charset=UTF-8")
					.PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
			if (internalToken != null && !internalToken.isBlank()) {
				req.header("X-Internal-Token", internalToken);
			}
			HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			int sc = resp.statusCode();
			if (sc < 200 || sc >= 300) {
				log.warn("[pms-api] {} 실패 status={} body={}", path, sc, resp.body());
				throw new ApiException(ApiStatus.SYSTEM_ERROR, "PMS API 호출 실패 (HTTP " + sc + ")");
			}
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			log.warn("[pms-api] {} 예외 {}", path, e.toString());
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "PMS API 호출 실패: " + e.getMessage());
		}
	}

	private static String esc(String v) {
		if (v == null) return "";
		return v.replace("\\", "\\\\").replace("\"", "\\\"");
	}
	private static String enc(String v) {
		return java.net.URLEncoder.encode(v, StandardCharsets.UTF_8);
	}
}
