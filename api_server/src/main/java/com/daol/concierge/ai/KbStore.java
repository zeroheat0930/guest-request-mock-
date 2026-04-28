package com.daol.concierge.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 호텔 지식베이스 (Knowledge Base) — RAG 챗봇용 문서 저장소.
 *
 * 부팅 시 {@code resources/kb/hotel.json} 로드. 시연 환경 기준
 * 14개 미만의 짧은 항목이라 in-memory + 토큰 기반 BM25-lite 점수로 충분.
 *
 * 검색 정책 (다국어 안전):
 *  1) 정규화된 쿼리에서 한글/영문/일본어/중국어 토큰 추출
 *  2) 각 문서의 title + tags + content 로 inverted-index 점수 계산
 *  3) 정확 매치(태그 / 짧은 토큰) 가중치 + 부분 매치 점수
 *  4) top-K 개 문서를 출처와 함께 리턴
 */
@Component
public class KbStore {

	private static final Logger log = LoggerFactory.getLogger(KbStore.class);

	public static class Doc {
		public final String id;
		public final String title;
		public final String section;
		public final String content;
		public final List<String> tags;
		final Set<String> tokens;        // title + tags + content 의 토큰 합집합 (소문자)
		final Set<String> bigrams;       // 부분문자열 매칭용 character bigram (한자/일본어 안전)

		Doc(String id, String title, String section, String content, List<String> tags) {
			this.id = id;
			this.title = title;
			this.section = section != null ? section : "";
			this.content = content;
			this.tags = tags != null ? tags : List.of();
			Set<String> toks = new HashSet<>();
			for (String t : tokenize(title)) toks.add(t);
			for (String t : tokens(this.tags.toArray(new String[0]))) toks.add(t);
			for (String t : tokenize(content)) toks.add(t);
			this.tokens = toks;
			this.bigrams = bigrams(title + " " + content);
		}
	}

	public static class Hit {
		public final Doc doc;
		public final double score;
		public Hit(Doc doc, double score) { this.doc = doc; this.score = score; }
	}

	private final List<Doc> docs = new ArrayList<>();

	@PostConstruct
	public void load() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			ClassPathResource res = new ClassPathResource("kb/hotel.json");
			if (!res.exists()) {
				log.warn("[kb] resources/kb/hotel.json 없음 — RAG 비활성");
				return;
			}
			try (InputStream is = res.getInputStream()) {
				JsonNode root = mapper.readTree(is);
				JsonNode arr = root.path("docs");
				if (!arr.isArray()) {
					log.warn("[kb] docs 배열이 아님");
					return;
				}
				for (JsonNode n : arr) {
					List<String> tags = new ArrayList<>();
					JsonNode ta = n.path("tags");
					if (ta.isArray()) ta.forEach(t -> tags.add(t.asText()));
					docs.add(new Doc(
							n.path("id").asText(""),
							n.path("title").asText(""),
							n.path("section").asText(""),
							n.path("content").asText(""),
							tags));
				}
				log.info("[kb] 호텔 KB 로드 완료 — {} docs", docs.size());
			}
		} catch (Exception e) {
			log.error("[kb] 로드 실패", e);
		}
	}

	public int size() { return docs.size(); }

	/** 다국어 안전 검색. K=top 개수. 점수 0 이하면 결과 없음. */
	public List<Hit> search(String query, int k) {
		if (query == null || query.isBlank() || docs.isEmpty()) return List.of();
		Set<String> qTokens = tokenize(query);
		Set<String> qBigrams = bigrams(query);
		List<Hit> hits = new ArrayList<>();
		for (Doc d : docs) {
			double s = score(d, qTokens, qBigrams, query.toLowerCase(Locale.ROOT));
			if (s > 0.0) hits.add(new Hit(d, s));
		}
		hits.sort(Comparator.comparingDouble((Hit h) -> -h.score));
		return hits.size() > k ? hits.subList(0, k) : hits;
	}

	private static double score(Doc d, Set<String> qTokens, Set<String> qBigrams, String qLower) {
		double s = 0;
		// (1) 태그 정확 매치 — 가중치 큼
		for (String tag : d.tags) {
			String t = tag.toLowerCase(Locale.ROOT);
			if (qLower.contains(t) || qTokens.contains(t)) s += 5;
		}
		// (2) 토큰 교집합
		for (String tok : qTokens) {
			if (d.tokens.contains(tok)) s += 1.0;
		}
		// (3) 제목 부분 매치
		String titleLower = d.title.toLowerCase(Locale.ROOT);
		for (String tok : qTokens) {
			if (titleLower.contains(tok)) s += 2.0;
		}
		// (4) bigram 매치 — CJK 안전
		int common = 0;
		for (String bg : qBigrams) {
			if (d.bigrams.contains(bg)) common++;
		}
		if (!qBigrams.isEmpty()) {
			s += 3.0 * ((double) common / qBigrams.size());
		}
		return s;
	}

	// ── 토큰화 / bigram ─────────────────────────────────────

	private static Set<String> tokenize(String text) {
		Set<String> out = new HashSet<>();
		if (text == null) return out;
		String lower = text.toLowerCase(Locale.ROOT);
		// 알파벳/숫자 토큰
		for (String w : lower.split("[^\\p{L}\\p{N}]+")) {
			if (w.length() >= 1) out.add(w);
		}
		return out;
	}

	private static Set<String> tokens(String... arr) {
		Set<String> out = new HashSet<>();
		for (String s : arr) out.addAll(tokenize(s));
		return out;
	}

	private static Set<String> bigrams(String text) {
		Set<String> out = new HashSet<>();
		if (text == null) return out;
		String norm = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
		for (int i = 0; i + 1 < norm.length(); i++) {
			char a = norm.charAt(i);
			char b = norm.charAt(i + 1);
			if (Character.isWhitespace(a) || Character.isWhitespace(b)) continue;
			out.add("" + a + b);
		}
		return out;
	}

	/** 어드민/디버그 용 — 모든 docs 메타 노출 */
	public List<Map<String, Object>> describeAll() {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Doc d : docs) {
			Map<String, Object> m = new HashMap<>();
			m.put("id", d.id);
			m.put("title", d.title);
			m.put("section", d.section);
			m.put("tags", d.tags);
			m.put("contentLen", d.content.length());
			out.add(m);
		}
		return out;
	}
}
