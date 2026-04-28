package com.daol.concierge.ai;

import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.inv.mapper.InvMapper;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 매니저 일일 AI 리포트 — 어제(또는 지정 일자) 운영 데이터를 Claude 가 1페이지 요약.
 *
 * 입력 데이터: CCS_TASK 일별 집계 + VOC 목록 + 당직 로그 + 분실물.
 * 출력: 마크다운 요약 (KPI 핫라이트, 이상 패턴, 추천 액션 3종).
 */
@Service
public class AiReportService {

	private static final Logger log = LoggerFactory.getLogger(AiReportService.class);

	@Autowired private InvMapper invMapper;

	@Value("${anthropic.api-key:}")
	private String apiKey;

	@Value("${anthropic.report.model:claude-sonnet-4-6}")
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

	/**
	 * @param propCd 프로퍼티 코드
	 * @param cmpxCd 컴플렉스 코드
	 * @param date   기준일 (yyyy-MM-dd). null 이면 어제
	 */
	public Map<String, Object> generate(String propCd, String cmpxCd, String date) {
		LocalDate targetDate = date != null && !date.isBlank() ? LocalDate.parse(date) : LocalDate.now().minusDays(1);
		String fromDt = targetDate + " 00:00:00";
		String toDt   = targetDate + " 23:59:59";

		// 1) 데이터 수집
		Map<String, Object> filter = new HashMap<>();
		filter.put("propCd", propCd);
		filter.put("cmpxCd", cmpxCd);
		filter.put("fromDt", fromDt);
		filter.put("toDt",   toDt);

		List<Map<String, Object>> daily = invMapper.selectDailyReport(filter);
		List<Map<String, Object>> sla   = invMapper.selectSlaReport(filter);

		Map<String, Object> vocFilter = new HashMap<>();
		vocFilter.put("propCd", propCd);
		vocFilter.put("cmpxCd", cmpxCd);
		List<Map<String, Object>> vocAll = invMapper.selectVocList(vocFilter);
		List<Map<String, Object>> voc = filterByDate(vocAll, "createdAt", targetDate);

		Map<String, Object> lfFilter = new HashMap<>();
		lfFilter.put("propCd", propCd);
		lfFilter.put("cmpxCd", cmpxCd);
		List<Map<String, Object>> lfAll = invMapper.selectLostFoundList(lfFilter);
		List<Map<String, Object>> lostfound = filterByDate(lfAll, "createdAt", targetDate);

		Map<String, Object> dutyFilter = new HashMap<>();
		dutyFilter.put("propCd", propCd);
		dutyFilter.put("cmpxCd", cmpxCd);
		dutyFilter.put("dutyDateFrom", targetDate.toString());
		dutyFilter.put("dutyDateTo",   targetDate.toString());
		List<Map<String, Object>> duty = invMapper.selectDutyLogList(dutyFilter);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("date", targetDate.toString());
		Map<String, Object> stats = new LinkedHashMap<>();
		stats.put("totalTasks", sumLongs(daily, "requestCount"));
		stats.put("doneTasks",  sumLongs(daily, "doneCount"));
		stats.put("vocCount",   voc.size());
		stats.put("lostFoundCount", lostfound.size());
		stats.put("dutyCount",  duty.size());
		result.put("stats", stats);
		result.put("dailyByDept", daily);
		result.put("slaBreakdown", sla);

		if (apiKey == null || apiKey.isBlank()) {
			result.put("summary", "## " + targetDate + " 일일 운영 요약\n\n" +
					"- 처리 태스크: " + stats.get("doneTasks") + " / " + stats.get("totalTasks") + "\n" +
					"- VOC 접수: " + stats.get("vocCount") + "건\n" +
					"- 분실물 신고: " + stats.get("lostFoundCount") + "건\n" +
					"- 당직 로그: " + stats.get("dutyCount") + "건\n\n" +
					"_(LLM 키 미설정 — 단순 통계 모드)_");
			result.put("model", "stats-only");
			return result;
		}

		// 2) Claude 호출용 프롬프트 빌드
		String userInput = buildPrompt(targetDate, daily, sla, voc, lostfound, duty);
		Map<String, Object> requestBody = new LinkedHashMap<>();
		requestBody.put("model", model);
		requestBody.put("max_tokens", 1024);
		requestBody.put("system", buildSystem());
		requestBody.put("messages", List.of(Map.of("role", "user", "content", userInput)));

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
				log.warn("[ai-report] error status={} body={}", res.statusCode(), res.body());
				throw new ApiException(ApiStatus.SYSTEM_ERROR, "LLM upstream error " + res.statusCode());
			}
			JsonNode root = mapper.readTree(res.body());
			String summary = root.path("content").path(0).path("text").asText("");
			result.put("summary", summary);
			result.put("model", model);
			log.info("[ai-report] {} stats={} model={}", targetDate, stats, model);
			return result;
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			log.error("[ai-report] failure", e);
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "AI report failed: " + e.getMessage());
		}
	}

	// ──────────────────────────────────────────────

	private static String buildSystem() {
		return "You are a hotel operations analyst. Given yesterday's CCS data, produce a concise " +
				"Korean markdown report with EXACTLY these sections:\n\n" +
				"## 📊 어제의 운영 요약\n" +
				"- 핵심 KPI 3-5 줄 (전체 처리율, 부서별 부하, SLA 준수율)\n\n" +
				"## ⚠️ 발견된 이상 패턴\n" +
				"- 데이터에서 직접 도출 가능한 이상치/패턴 2-3 개. 없으면 '특이 사항 없음' 으로.\n" +
				"- 예: 같은 객실에서 3건 이상 신고 / 특정 시간대 부하 집중 / SLA 미준수 비율 급상승\n\n" +
				"## 🎯 권장 액션\n" +
				"- 매니저가 오늘 즉시 해야 할 액션 3개 (구체, 부서/대상 명시)\n\n" +
				"한국어로, 짧고 명확하게. 데이터에 없는 추측은 하지 말 것. " +
				"숫자는 데이터에 있는 그대로 인용.";
	}

	private static String buildPrompt(LocalDate date, List<Map<String, Object>> daily,
	                                   List<Map<String, Object>> sla, List<Map<String, Object>> voc,
	                                   List<Map<String, Object>> lostfound, List<Map<String, Object>> duty) {
		StringBuilder sb = new StringBuilder();
		sb.append("기준일: ").append(date).append("\n\n");

		sb.append("== 부서·요청유형별 일일 집계 ==\n");
		if (daily.isEmpty()) sb.append("(데이터 없음)\n");
		else for (Map<String, Object> r : daily) {
			sb.append("- ").append(r.get("deptCd")).append(" / ").append(r.get("sourceType"))
					.append(": req=").append(r.get("requestCount"))
					.append(", done=").append(r.get("doneCount"))
					.append(", avgMin=").append(r.get("avgElapsedMin")).append("\n");
		}

		sb.append("\n== SLA 집계 ==\n");
		if (sla.isEmpty()) sb.append("(데이터 없음)\n");
		else for (Map<String, Object> r : sla) {
			sb.append("- ").append(r.get("deptCd")).append(" / ").append(r.get("sourceType"))
					.append(": total=").append(r.get("totalCount"))
					.append(", done=").append(r.get("doneCount"))
					.append(", avgMin=").append(r.get("avgElapsedMin"))
					.append(", maxMin=").append(r.get("maxElapsedMin")).append("\n");
		}

		sb.append("\n== VOC (").append(voc.size()).append("건) ==\n");
		for (int i = 0; i < voc.size() && i < 20; i++) {
			Map<String, Object> v = voc.get(i);
			sb.append("- [").append(v.get("severity")).append("/").append(v.get("category"))
					.append("] room=").append(safe(v.get("rmNo")))
					.append(" status=").append(v.get("statusCd"))
					.append(" content=").append(trim(safe(v.get("content")), 80)).append("\n");
		}

		sb.append("\n== 분실물 (").append(lostfound.size()).append("건) ==\n");
		for (int i = 0; i < lostfound.size() && i < 10; i++) {
			Map<String, Object> l = lostfound.get(i);
			sb.append("- [").append(l.get("category")).append("] ")
					.append(safe(l.get("itemName")))
					.append(" status=").append(l.get("statusCd")).append("\n");
		}

		sb.append("\n== 당직 로그 (").append(duty.size()).append("건) ==\n");
		for (Map<String, Object> d : duty) {
			sb.append("- ").append(d.get("shiftCd")).append(" / mgr=").append(safe(d.get("managerId")))
					.append(", summary=").append(trim(safe(d.get("summary")), 100))
					.append(", incidents=").append(trim(safe(d.get("incidents")), 100)).append("\n");
		}

		sb.append("\n위 데이터를 기반으로 운영 리포트를 작성하세요.");
		return sb.toString();
	}

	private static List<Map<String, Object>> filterByDate(List<Map<String, Object>> rows, String key, LocalDate date) {
		java.util.ArrayList<Map<String, Object>> out = new java.util.ArrayList<>();
		String prefix = date.toString();
		for (Map<String, Object> r : rows) {
			Object v = r.get(key);
			if (v == null) continue;
			String s = String.valueOf(v);
			if (s.startsWith(prefix)) out.add(r);
		}
		return out;
	}

	private static long sumLongs(List<Map<String, Object>> rows, String key) {
		long s = 0;
		for (Map<String, Object> r : rows) {
			Object v = r.get(key);
			if (v instanceof Number) s += ((Number) v).longValue();
			else if (v != null) try { s += Long.parseLong(String.valueOf(v)); } catch (Exception ignore) {}
		}
		return s;
	}

	private static String safe(Object o) { return o == null ? "" : String.valueOf(o); }
	private static String trim(String s, int n) { return s.length() > n ? s.substring(0, n) + "…" : s; }
}
