package com.daol.concierge.ai;

import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiStatus;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 다국어 양방향 통역 서비스 — 게스트 ↔ 스태프.
 *
 * 입력 텍스트의 언어를 자동 감지하고 target 언어로 번역.
 * 호텔 도메인 톤 유지 (정중한 응대 / 운영 용어).
 *
 * 지원 언어: ko / en / ja / zh (4개국어 페어 = 12 페어).
 */
@Service
public class AiTranslateService {

	private static final Logger log = LoggerFactory.getLogger(AiTranslateService.class);

	private static final List<String> SUPPORTED = List.of("ko", "en", "ja", "zh");

	@Value("${anthropic.api-key:}")
	private String apiKey;

	@Value("${anthropic.translate.model:claude-haiku-4-5-20251001}")
	private String model;

	@Value("${anthropic.endpoint:https://api.anthropic.com/v1/messages}")
	private String endpoint;

	@Value("${anthropic.version:2023-06-01}")
	private String anthropicVersion;

	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	private final ObjectMapper mapper = new ObjectMapper();

	public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

	@SuppressWarnings("unchecked")
	public Map<String, Object> translate(Map<String, Object> params) {
		if (!isConfigured()) {
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "LLM API key not configured");
		}
		Object t = params != null ? params.get("text") : null;
		if (!(t instanceof String) || ((String) t).isBlank()) {
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "text 필수");
		}
		String text = (String) t;
		String target = normalize((String) params.getOrDefault("targetLang", "ko"));
		String source = normalize((String) params.getOrDefault("sourceLang", "auto"));
		if (!SUPPORTED.contains(target)) {
			throw new ApiException(ApiStatus.BAD_REQUEST, "지원하지 않는 targetLang: " + target);
		}
		String tone = (String) params.getOrDefault("tone", "polite_hotel");

		String systemPrompt = buildSystem(source, target, tone);

		Map<String, Object> requestBody = new LinkedHashMap<>();
		requestBody.put("model", model);
		requestBody.put("max_tokens", 512);
		requestBody.put("system", systemPrompt);
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
				log.warn("[ai-translate] error status={} body={}", res.statusCode(), res.body());
				throw new ApiException(ApiStatus.SYSTEM_ERROR, "LLM upstream error " + res.statusCode());
			}
			JsonNode root = mapper.readTree(res.body());
			String translated = root.path("content").path(0).path("text").asText("").trim();

			Map<String, Object> result = new LinkedHashMap<>();
			result.put("text", text);
			result.put("translated", translated);
			result.put("sourceLang", source);
			result.put("targetLang", target);
			result.put("model", model);
			log.info("[ai-translate] {}->{} len={}->{} model={}", source, target, text.length(), translated.length(), model);
			return result;
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			log.error("[ai-translate] failure", e);
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "Translate failed: " + e.getMessage());
		}
	}

	private static String buildSystem(String source, String target, String tone) {
		String sourceLine = "auto".equals(source) ? "Detect the source language automatically." :
				"Source language: " + langName(source) + ".";
		String toneLine = "polite_hotel".equals(tone)
				? "Use a polite, professional hotel-staff tone (정중한 호텔 응대 톤). " +
				  "Use honorifics where appropriate (Korean: -십니다, Japanese: です/ます, Chinese: 您/您们)."
				: "Use a neutral, accurate tone.";
		return "You are a professional hotel concierge translator. " +
				sourceLine + " Translate the user's message into " + langName(target) + ". " +
				toneLine + " " +
				"OUTPUT ONLY the translated text — no quotes, no source label, no commentary, no markdown.";
	}

	private static String langName(String code) {
		switch (code) {
			case "ko": return "Korean (한국어)";
			case "en": return "English";
			case "ja": return "Japanese (日本語)";
			case "zh": return "Simplified Chinese (简体中文)";
			default:   return code;
		}
	}

	private static String normalize(String lang) {
		if (lang == null) return "ko";
		String s = lang.toLowerCase();
		if (s.startsWith("ko")) return "ko";
		if (s.startsWith("en")) return "en";
		if (s.startsWith("ja")) return "ja";
		if (s.startsWith("zh")) return "zh";
		if ("auto".equals(s)) return "auto";
		return s;
	}
}
