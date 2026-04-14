package com.daol.concierge.ai;

import com.daol.concierge.auth.SecurityContextUtil;
import com.daol.concierge.core.api.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Anthropic API 프록시 서비스
 *
 * 프론트엔드의 자연어 텍스트를 받아 Claude API로 의도 파싱 JSON을 얻어서 반환.
 * API 키는 서버 환경변수(ANTHROPIC_API_KEY)에서만 읽으며, 프론트에는 절대 노출 안 됨.
 */
@Service
public class AiChatService {

	private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

	private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*}");

	@Value("${anthropic.api-key:}")
	private String apiKey;

	@Value("${anthropic.model:claude-haiku-4-5-20251001}")
	private String model;

	@Value("${anthropic.endpoint:https://api.anthropic.com/v1/messages}")
	private String endpoint;

	@Value("${anthropic.version:2023-06-01}")
	private String anthropicVersion;

	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	private final ObjectMapper mapper = new ObjectMapper();

	public boolean isConfigured() {
		return apiKey != null && !apiKey.isBlank();
	}

	public String getModel() {
		return model;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> parseIntent(Map<String, Object> params) {
		// defense-in-depth: SecurityConfig 의 filter chain 이 이미 인증을 강제하지만,
		// 이후 누군가 /api/ai/chat 을 실수로 permitAll 로 풀어도 서비스 레이어에서 한 번 더 막는다.
		SecurityContextUtil.requirePrincipal();
		if (!isConfigured()) {
			throw new BizException("9501", "LLM API key not configured");
		}
		if (params == null) {
			throw new BizException("9001", "params 필수");
		}
		Object textObj = params.get("text");
		if (!(textObj instanceof String) || ((String) textObj).isBlank()) {
			throw new BizException("9001", "text 필수");
		}
		String text = (String) textObj;

		Map<String, Object> ctx;
		Object ctxObj = params.get("ctx");
		if (ctxObj instanceof Map) {
			ctx = (Map<String, Object>) ctxObj;
		} else {
			ctx = new HashMap<>();
		}
		// 인증된 게스트의 rsvNo 외의 컨텍스트는 거절 (프롬프트 인젝션/타 예약 정보 누출 방지)
		String ctxRsvNo = (String) ctx.get("rsvNo");
		if (ctxRsvNo != null && !ctxRsvNo.isEmpty()) {
			SecurityContextUtil.assertOwnsRsv(ctxRsvNo);
		}
		String lang = (String) ctx.getOrDefault("perUseLang", "ko_KR");

		String system = buildSystemPrompt(ctx, lang);

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("model", model);
		requestBody.put("max_tokens", 512);
		requestBody.put("system", system);
		requestBody.put("messages", List.of(Map.of("role", "user", "content", text)));

		try {
			String body = mapper.writeValueAsString(requestBody);
			HttpRequest req = HttpRequest.newBuilder()
					.uri(URI.create(endpoint))
					.timeout(Duration.ofSeconds(15))
					.header("content-type", "application/json")
					.header("x-api-key", apiKey)
					.header("anthropic-version", anthropicVersion)
					.POST(HttpRequest.BodyPublishers.ofString(body))
					.build();

			HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() != 200) {
				log.warn("Anthropic API error status={} body={}", res.statusCode(), res.body());
				throw new BizException("9502", "LLM upstream error " + res.statusCode());
			}

			JsonNode root = mapper.readTree(res.body());
			String rawText = root.path("content").path(0).path("text").asText("");
			Matcher m = JSON_BLOCK.matcher(rawText);
			if (!m.find()) {
				throw new BizException("9503", "LLM did not return JSON");
			}
			Map<String, Object> parsed = mapper.readValue(m.group(), Map.class);
			if (!parsed.containsKey("intent")) {
				throw new BizException("9503", "LLM response missing 'intent'");
			}
			return parsed;
		} catch (BizException e) {
			throw e;
		} catch (Exception e) {
			log.error("AiChatService parseIntent failure", e);
			throw new BizException("9502", "LLM call failed: " + e.getMessage());
		}
	}

	private String buildSystemPrompt(Map<String, Object> ctx, String lang) {
		return "You are a hotel concierge AI. Parse the guest's message into JSON.\n\n" +
				"Reservation context:\n" +
				"- rsvNo: " + ctx.getOrDefault("rsvNo", "") + "\n" +
				"- roomNo: " + ctx.getOrDefault("roomNo", "") + "\n" +
				"- current checkout time: " + ctx.getOrDefault("chkOutTm", "") + "\n" +
				"- guest language: " + lang + "\n\n" +
				"Output STRICT JSON only (no prose, no markdown):\n" +
				"{\n" +
				"  \"intent\": \"amenity\" | \"housekeeping\" | \"late_checkout\" | \"chat\",\n" +
				"  \"reply\": \"<short reply in " + lang + ">\",\n" +
				"  \"payload\": {\n" +
				"    // for amenity: { \"itemList\": [{\"itemCd\":\"AM001\",\"qty\":2}], \"reqMemo\":\"...\" }\n" +
				"    //   item codes: AM001=towel, AM002=water, AM003=soap, AM004=shampoo, AM005=toothbrush\n" +
				"    // for housekeeping: { \"hkStatCd\":\"MU\"|\"DND\"|\"CLR\", \"reqMemo\":\"...\" }\n" +
				"    // for late_checkout: { \"reqOutTm\":\"HHMM\" }\n" +
				"    // for chat: {}\n" +
				"  }\n" +
				"}\n\n" +
				"Guidelines:\n" +
				"- Guest complaints about room temperature, noise, broken items, or anything requiring staff action should route to housekeeping with a descriptive reqMemo.\n" +
				"- Requests for extra pillows, blankets, slippers, or items not in the standard AM001-AM005 list should also route to housekeeping with reqMemo.\n" +
				"- Only route to amenity when the item clearly maps to one of AM001-AM005.";
	}
}
