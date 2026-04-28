package com.daol.concierge.ai;

import com.daol.concierge.auth.SecurityContextUtil;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Claude Tool Use 자율 에이전트 — 게스트 자연어 한 줄에서 여러 호텔 액션을 동시 추출.
 *
 * 기존 {@link AiChatService} 가 단일 의도(JSON 파싱) 모드라면, 본 서비스는
 * Anthropic Tool Use(Function Calling) API 를 사용해 Claude 가 1~N 개의
 * 호텔 도구를 자율적으로 호출하게 한다.
 *
 * 도구 목록:
 *   - request_amenity        — 어메니티 (수건/물/비누/샴푸/칫솔)
 *   - set_housekeeping       — 하우스키핑 상태 (MU/DND/CLR)
 *   - request_late_checkout  — 레이트 체크아웃 시간
 *   - report_lostfound       — 분실물 신고
 *   - submit_voc             — VOC/불편사항
 *   - request_rental         — 물품 대여
 *   - register_parking       — 주차 차량 등록
 *   - general_chat           — 일반 대화 (액션 없음)
 *
 * 응답 포맷 (프론트 호환):
 *   { actions: [{ intent, payload }, ...], reply: string, model: string }
 */
@Service
public class AiAgentService {

	private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

	@Value("${anthropic.api-key:}")
	private String apiKey;

	@Value("${anthropic.agent.model:claude-sonnet-4-6}")
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
	public String getModel() { return model; }

	@SuppressWarnings("unchecked")
	public Map<String, Object> run(Map<String, Object> params) {
		SecurityContextUtil.requirePrincipal();
		if (!isConfigured()) {
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "LLM API key not configured");
		}
		Object textObj = params != null ? params.get("text") : null;
		if (!(textObj instanceof String) || ((String) textObj).isBlank()) {
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "text 필수");
		}
		String text = (String) textObj;

		Map<String, Object> ctx = new HashMap<>();
		Object ctxObj = params.get("ctx");
		if (ctxObj instanceof Map) ctx.putAll((Map<String, Object>) ctxObj);
		String ctxRsvNo = (String) ctx.get("rsvNo");
		if (ctxRsvNo != null && !ctxRsvNo.isEmpty()) {
			SecurityContextUtil.assertOwnsRsv(ctxRsvNo);
		}
		String lang = (String) ctx.getOrDefault("perUseLang", "ko_KR");

		Map<String, Object> requestBody = new LinkedHashMap<>();
		requestBody.put("model", model);
		requestBody.put("max_tokens", 1024);
		requestBody.put("system", buildSystemPrompt(ctx, lang));
		requestBody.put("tools", buildTools());
		requestBody.put("messages", List.of(Map.of("role", "user", "content", text)));

		try {
			String body = mapper.writeValueAsString(requestBody);
			HttpRequest req = HttpRequest.newBuilder()
					.uri(URI.create(endpoint))
					.timeout(Duration.ofSeconds(30))
					.header("content-type", "application/json")
					.header("x-api-key", apiKey)
					.header("anthropic-version", anthropicVersion)
					.POST(HttpRequest.BodyPublishers.ofString(body))
					.build();

			HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() != 200) {
				log.warn("Anthropic agent error status={} body={}", res.statusCode(), res.body());
				throw new ApiException(ApiStatus.SYSTEM_ERROR, "LLM upstream error " + res.statusCode());
			}

			JsonNode root = mapper.readTree(res.body());
			JsonNode contentArr = root.path("content");

			StringBuilder replyBuf = new StringBuilder();
			List<Map<String, Object>> actions = new ArrayList<>();
			if (contentArr.isArray()) {
				for (JsonNode block : contentArr) {
					String type = block.path("type").asText("");
					if ("text".equals(type)) {
						String txt = block.path("text").asText("");
						if (!txt.isBlank()) {
							if (replyBuf.length() > 0) replyBuf.append(" ");
							replyBuf.append(txt.trim());
						}
					} else if ("tool_use".equals(type)) {
						String tool = block.path("name").asText("");
						JsonNode inputNode = block.path("input");
						Map<String, Object> input = mapper.convertValue(inputNode, Map.class);
						Map<String, Object> action = mapToAction(tool, input != null ? input : new HashMap<>());
						if (action != null) actions.add(action);
					}
				}
			}

			Map<String, Object> result = new LinkedHashMap<>();
			result.put("actions", actions);
			result.put("reply", replyBuf.length() > 0 ? replyBuf.toString() : null);
			result.put("model", model);
			result.put("stopReason", root.path("stop_reason").asText(""));
			log.info("[ai-agent] {} actions, model={}, stop={}", actions.size(), model, root.path("stop_reason").asText(""));
			return result;
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			log.error("AiAgentService run failure", e);
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "LLM agent call failed: " + e.getMessage());
		}
	}

	/** Tool 입력을 기존 ChatView 가 처리하는 intent + payload 포맷으로 정규화. */
	private static Map<String, Object> mapToAction(String tool, Map<String, Object> input) {
		Map<String, Object> a = new LinkedHashMap<>();
		switch (tool) {
			case "request_amenity": {
				a.put("intent", "amenity");
				Map<String, Object> p = new LinkedHashMap<>();
				p.put("itemList", input.getOrDefault("items", List.of()));
				p.put("reqMemo", input.getOrDefault("memo", ""));
				a.put("payload", p);
				return a;
			}
			case "set_housekeeping": {
				a.put("intent", "housekeeping");
				Map<String, Object> p = new LinkedHashMap<>();
				p.put("hkStatCd", input.getOrDefault("status", "MU"));
				p.put("reqMemo", input.getOrDefault("memo", ""));
				a.put("payload", p);
				return a;
			}
			case "request_late_checkout": {
				a.put("intent", "late_checkout");
				Map<String, Object> p = new LinkedHashMap<>();
				p.put("reqOutTm", input.getOrDefault("reqOutTm", "1300"));
				a.put("payload", p);
				return a;
			}
			case "report_lostfound": {
				a.put("intent", "lostfound");
				Map<String, Object> p = new LinkedHashMap<>(input);
				a.put("payload", p);
				return a;
			}
			case "submit_voc": {
				a.put("intent", "voc");
				Map<String, Object> p = new LinkedHashMap<>(input);
				p.putIfAbsent("severity", "NORMAL");
				a.put("payload", p);
				return a;
			}
			case "request_rental": {
				a.put("intent", "rental");
				Map<String, Object> p = new LinkedHashMap<>(input);
				a.put("payload", p);
				return a;
			}
			case "register_parking": {
				a.put("intent", "parking");
				Map<String, Object> p = new LinkedHashMap<>(input);
				a.put("payload", p);
				return a;
			}
			case "general_chat": {
				a.put("intent", "chat");
				a.put("payload", new LinkedHashMap<>(input));
				return a;
			}
			default:
				return null;
		}
	}

	private static String buildSystemPrompt(Map<String, Object> ctx, String lang) {
		return "You are an autonomous hotel concierge agent. The guest writes a message in their language (" + lang + "). " +
				"Identify ALL distinct requests in the message and call the appropriate tools — one tool_use block per distinct request. " +
				"A single message may contain multiple requests (e.g. amenity + late checkout + housekeeping all at once). " +
				"After calling tools, also produce a short friendly reply text in " + lang + " confirming what you'll handle.\n\n" +
				"Reservation context:\n" +
				"  rsvNo:     " + ctx.getOrDefault("rsvNo", "") + "\n" +
				"  roomNo:    " + ctx.getOrDefault("roomNo", "") + "\n" +
				"  current checkout: " + ctx.getOrDefault("chkOutTm", "") + "\n\n" +
				"Tool selection guidelines:\n" +
				"  - Standard amenity items only: AM001=towel, AM002=water, AM003=soap, AM004=shampoo, AM005=toothbrush. " +
				"For non-standard items (extra pillow, blanket, slipper) use set_housekeeping with descriptive memo instead.\n" +
				"  - Complaints about temperature/noise/broken items → submit_voc with severity HIGH for safety/health, " +
				"NORMAL otherwise.\n" +
				"  - Lost item → report_lostfound. Be sure to include category and itemName.\n" +
				"  - Late checkout reqOutTm format is HHMM (e.g. \"1300\").\n" +
				"  - When the guest is just chatting/asking a question with no action needed, call general_chat once.\n" +
				"  - Do NOT call any tool more than once per distinct request.";
	}

	private static List<Map<String, Object>> buildTools() {
		List<Map<String, Object>> tools = new ArrayList<>();
		tools.add(Map.of(
				"name", "request_amenity",
				"description", "Request standard amenity items (towel, water, soap, shampoo, toothbrush). Use this only for AM001-AM005.",
				"input_schema", Map.of(
						"type", "object",
						"properties", Map.of(
								"items", Map.of(
										"type", "array",
										"description", "List of {itemCd, qty} entries",
										"items", Map.of(
												"type", "object",
												"properties", Map.of(
														"itemCd", Map.of("type", "string", "enum", List.of("AM001","AM002","AM003","AM004","AM005")),
														"qty", Map.of("type", "integer", "minimum", 1, "maximum", 10)
												),
												"required", List.of("itemCd", "qty")
										)
								),
								"memo", Map.of("type", "string", "description", "Original guest text or extra note")
						),
						"required", List.of("items")
				)
		));
		tools.add(Map.of(
				"name", "set_housekeeping",
				"description", "Set housekeeping status for the guest's room or request non-standard room services (extra pillow, blanket, slipper, broken item, temperature complaint, etc.)",
				"input_schema", Map.of(
						"type", "object",
						"properties", Map.of(
								"status", Map.of("type", "string", "enum", List.of("MU","DND","CLR"),
										"description", "MU=make up room, DND=do not disturb, CLR=clear DND"),
								"memo", Map.of("type", "string", "description", "Specific request, e.g. '베개 1개 추가'")
						),
						"required", List.of("status")
				)
		));
		tools.add(Map.of(
				"name", "request_late_checkout",
				"description", "Request late checkout extension. Returns price quote, guest confirms separately.",
				"input_schema", Map.of(
						"type", "object",
						"properties", Map.of(
								"reqOutTm", Map.of("type", "string", "pattern", "^[0-9]{4}$",
										"description", "Requested checkout time in HHMM (e.g. '1300' for 1pm)")
						),
						"required", List.of("reqOutTm")
				)
		));
		tools.add(Map.of(
				"name", "report_lostfound",
				"description", "Report a lost or missing item.",
				"input_schema", Map.of(
						"type", "object",
						"properties", Map.of(
								"category", Map.of("type", "string", "enum", List.of("ELEC","DOC","BAG","CLOTH","JEWEL","OTHER")),
								"itemName", Map.of("type", "string"),
								"description", Map.of("type", "string"),
								"locationHint", Map.of("type", "string")
						),
						"required", List.of("itemName")
				)
		));
		tools.add(Map.of(
				"name", "submit_voc",
				"description", "Submit a guest complaint or feedback.",
				"input_schema", Map.of(
						"type", "object",
						"properties", Map.of(
								"category", Map.of("type", "string", "enum", List.of("ROOM","FB","FACILITY","STAFF","NOISE","OTHER")),
								"severity", Map.of("type", "string", "enum", List.of("LOW","NORMAL","HIGH","URGENT")),
								"title", Map.of("type", "string"),
								"content", Map.of("type", "string")
						),
						"required", List.of("content")
				)
		));
		tools.add(Map.of(
				"name", "request_rental",
				"description", "Request item rental from hotel catalog (umbrella, charger, adapter, iron, etc.)",
				"input_schema", Map.of(
						"type", "object",
						"properties", Map.of(
								"itemName", Map.of("type", "string", "description", "Free-form item name guest is asking for"),
								"qty", Map.of("type", "integer", "minimum", 1),
								"note", Map.of("type", "string")
						),
						"required", List.of("itemName")
				)
		));
		tools.add(Map.of(
				"name", "register_parking",
				"description", "Register guest's parked vehicle for valet/parking service.",
				"input_schema", Map.of(
						"type", "object",
						"properties", Map.of(
								"carNo", Map.of("type", "string"),
								"carTp", Map.of("type", "string", "description", "e.g. SEDAN, SUV"),
								"memo", Map.of("type", "string")
						),
						"required", List.of("carNo")
				)
		));
		tools.add(Map.of(
				"name", "general_chat",
				"description", "Use ONLY when the guest is making small talk or asking a question that does not require any hotel action.",
				"input_schema", Map.of(
						"type", "object",
						"properties", Map.of(
								"topic", Map.of("type", "string")
						)
				)
		));
		return tools;
	}
}
