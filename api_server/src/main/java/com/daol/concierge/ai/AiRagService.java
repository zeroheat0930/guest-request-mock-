package com.daol.concierge.ai;

import com.daol.concierge.auth.SecurityContextUtil;
import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 호텔 챗봇 — KB 검색 + Claude 답변 + 출처 인용.
 *
 * 흐름:
 *  1) {@link KbStore} 에서 질문에 가장 가까운 docs top-K (기본 4)
 *  2) 검색 결과를 system prompt 의 컨텍스트로 주입
 *  3) Claude 에게 "주어진 컨텍스트로만 답변, 모르면 모른다고" 지시
 *  4) 답변 + 인용한 docId 리스트 반환
 *
 * KB 미로드/Claude 키 미설정 시 fallback 응답 ('정보 없음') 으로 graceful degrade.
 */
@Service
public class AiRagService {

	private static final Logger log = LoggerFactory.getLogger(AiRagService.class);

	@Autowired private KbStore kbStore;

	@Value("${anthropic.api-key:}")
	private String apiKey;

	@Value("${anthropic.rag.model:claude-haiku-4-5-20251001}")
	private String model;

	@Value("${anthropic.endpoint:https://api.anthropic.com/v1/messages}")
	private String endpoint;

	@Value("${anthropic.version:2023-06-01}")
	private String anthropicVersion;

	@Value("${anthropic.rag.topK:4}")
	private int topK;

	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	private final ObjectMapper mapper = new ObjectMapper();

	public boolean isConfigured() {
		return apiKey != null && !apiKey.isBlank() && kbStore.size() > 0;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> ask(Map<String, Object> params) {
		SecurityContextUtil.requirePrincipal();
		Object q = params != null ? params.get("query") : null;
		if (!(q instanceof String) || ((String) q).isBlank()) {
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "query 필수");
		}
		String query = (String) q;
		Map<String, Object> ctx = new HashMap<>();
		Object ctxObj = params.get("ctx");
		if (ctxObj instanceof Map) ctx.putAll((Map<String, Object>) ctxObj);
		String lang = (String) ctx.getOrDefault("perUseLang", "ko_KR");

		List<KbStore.Hit> hits = kbStore.search(query, topK);
		if (hits.isEmpty()) {
			Map<String, Object> r = new LinkedHashMap<>();
			r.put("answer", lang.startsWith("en") ? "Sorry, I couldn't find that information."
					: lang.startsWith("ja") ? "申し訳ありません、その情報は見つかりませんでした。"
					: lang.startsWith("zh") ? "抱歉，没有找到相关信息。"
					: "죄송합니다, 해당 정보를 찾지 못했습니다.");
			r.put("citations", List.of());
			r.put("model", model);
			r.put("hits", 0);
			return r;
		}

		// KB 만 있고 LLM 키 없으면 — 가장 점수 높은 문서를 그대로 반환
		if (apiKey == null || apiKey.isBlank()) {
			KbStore.Doc top = hits.get(0).doc;
			Map<String, Object> r = new LinkedHashMap<>();
			r.put("answer", top.content);
			r.put("citations", buildCitations(hits));
			r.put("model", "kb-only");
			r.put("hits", hits.size());
			return r;
		}

		String contextBlock = buildContextBlock(hits);
		String system = buildSystemPrompt(lang);
		String userMessage = "Question: " + query + "\n\n" + contextBlock;

		Map<String, Object> requestBody = new LinkedHashMap<>();
		requestBody.put("model", model);
		requestBody.put("max_tokens", 512);
		requestBody.put("system", system);
		requestBody.put("messages", List.of(Map.of("role", "user", "content", userMessage)));

		try {
			String body = mapper.writeValueAsString(requestBody);
			HttpRequest req = HttpRequest.newBuilder()
					.uri(URI.create(endpoint))
					.timeout(Duration.ofSeconds(20))
					.header("content-type", "application/json")
					.header("x-api-key", apiKey)
					.header("anthropic-version", anthropicVersion)
					.POST(HttpRequest.BodyPublishers.ofString(body))
					.build();

			HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() != 200) {
				log.warn("[rag] Anthropic error status={} body={}", res.statusCode(), res.body());
				throw new ApiException(ApiStatus.SYSTEM_ERROR, "LLM upstream error " + res.statusCode());
			}
			JsonNode root = mapper.readTree(res.body());
			String answer = root.path("content").path(0).path("text").asText("");

			Map<String, Object> r = new LinkedHashMap<>();
			r.put("answer", answer);
			r.put("citations", buildCitations(hits));
			r.put("model", model);
			r.put("hits", hits.size());
			log.info("[rag] q='{}' hits={} model={}", trim(query, 60), hits.size(), model);
			return r;
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			log.error("[rag] call failed", e);
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "RAG call failed: " + e.getMessage());
		}
	}

	private static String buildContextBlock(List<KbStore.Hit> hits) {
		StringBuilder sb = new StringBuilder();
		sb.append("Hotel knowledge base (use ONLY these to answer):\n\n");
		for (int i = 0; i < hits.size(); i++) {
			KbStore.Doc d = hits.get(i).doc;
			sb.append("[#").append(i + 1).append(" id=").append(d.id);
			if (!d.section.isEmpty()) sb.append(" section=").append(d.section);
			sb.append("] ").append(d.title).append("\n");
			sb.append(d.content).append("\n\n");
		}
		return sb.toString();
	}

	private static List<Map<String, Object>> buildCitations(List<KbStore.Hit> hits) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (KbStore.Hit h : hits) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("docId", h.doc.id);
			m.put("title", h.doc.title);
			m.put("section", h.doc.section);
			m.put("score", Math.round(h.score * 100.0) / 100.0);
			out.add(m);
		}
		return out;
	}

	private static String buildSystemPrompt(String lang) {
		return "You are a hotel concierge AI. Answer the guest's question using ONLY the knowledge base " +
				"snippets provided. Reply in the guest's language: " + lang + ". " +
				"Be concise (2-3 sentences). " +
				"At the end of the answer, on a new line, append source markers in this format: [출처: §section] " +
				"(use the section field from the snippets). " +
				"If the answer is not in the knowledge base, say so honestly in the guest's language and suggest contacting the front desk.";
	}

	private static String trim(String s, int n) {
		if (s == null) return "";
		return s.length() > n ? s.substring(0, n) + "…" : s;
	}
}
