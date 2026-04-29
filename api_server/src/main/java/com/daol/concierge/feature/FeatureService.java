package com.daol.concierge.feature;

import com.daol.concierge.inv.mapper.InvMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeatureService {

	private static final Logger logger = LoggerFactory.getLogger(FeatureService.class);

	@Autowired private InvMapper invMapper;
	@Autowired private ObjectMapper objectMapper;

	public List<Map<String, Object>> listForGuest(String propCd, String cmpxCd) {
		return invMapper.selectEnabledFeatures(propCd, cmpxCd);
	}

	public List<Map<String, Object>> listForAdmin(String propCd, String cmpxCd) {
		return invMapper.selectFeatures(propCd, cmpxCd);
	}

	public List<Map<String, Object>> upsertBulk(String propCd, String cmpxCd, List<Map<String, Object>> rows) {
		for (Map<String, Object> row : rows) {
			String featureCd = row.get("featureCd") == null ? null : String.valueOf(row.get("featureCd"));
			if (featureCd == null || featureCd.isBlank()) continue;

			Map<String, Object> param = new HashMap<>();
			param.put("propCd", propCd);
			param.put("cmpxCd", cmpxCd);
			param.put("featureCd", featureCd);
			param.put("useYn", row.getOrDefault("useYn", "N"));
			param.put("sortOrd", row.getOrDefault("sortOrd", 99));
			// configJson 이 Map/List 로 들어오면 JSON 직렬화. String 이면 그대로. null 이면 null.
			param.put("configJson", serializeConfig(row.get("configJson")));
			param.put("regUser", "ADMIN");
			param.put("modUser", "ADMIN");

			int updated = invMapper.updateFeature(param);
			if (updated == 0) {
				invMapper.insertFeature(param);
			}
		}
		return listForAdmin(propCd, cmpxCd);
	}

	/**
	 * 특정 기능의 configJson 을 Map 으로 반환. 미설정/파싱 실패 시 빈 Map.
	 * 도메인 코드(NearbyController, GrService 등)가 호텔별 커스터마이징을 적용할 때 사용.
	 */
	public Map<String, Object> getConfig(String propCd, String cmpxCd, String featureCd) {
		List<Map<String, Object>> all = invMapper.selectFeatures(propCd, cmpxCd);
		if (all == null) return Collections.emptyMap();
		for (Map<String, Object> row : all) {
			if (featureCd.equals(row.get("featureCd"))) {
				Object raw = row.get("configJson");
				if (raw == null) return Collections.emptyMap();
				if (raw instanceof Map<?, ?> m) {
					@SuppressWarnings("unchecked")
					Map<String, Object> cast = (Map<String, Object>) m;
					return cast;
				}
				try {
					String json = String.valueOf(raw).trim();
					if (json.isEmpty() || !json.startsWith("{")) return Collections.emptyMap();
					@SuppressWarnings("unchecked")
					Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
					return parsed;
				} catch (Exception e) {
					logger.warn("[feature.getConfig] configJson 파싱 실패 prop={} cmpx={} feat={}: {}",
							propCd, cmpxCd, featureCd, e.getMessage());
					return Collections.emptyMap();
				}
			}
		}
		return Collections.emptyMap();
	}

	private String serializeConfig(Object configJson) {
		if (configJson == null) return null;
		if (configJson instanceof String s) {
			return s.isBlank() ? null : s;
		}
		try {
			return objectMapper.writeValueAsString(configJson);
		} catch (JsonProcessingException e) {
			logger.warn("[feature.serializeConfig] 직렬화 실패: {}", e.getMessage());
			return null;
		}
	}
}
