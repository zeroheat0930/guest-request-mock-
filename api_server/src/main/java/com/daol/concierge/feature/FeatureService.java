package com.daol.concierge.feature;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 컨시어지 기능 플래그 서비스
 *
 * 게스트 API: useYn='Y' 만 반환. 관리자 API: 전체 반환 + bulk upsert.
 */
@Service
public class FeatureService {

	@Autowired private ConciergeFeatureRepository featureRepo;

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listForGuest(String propCd) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (ConciergeFeature f : featureRepo.findByPropCdOrderBySortOrdAsc(propCd)) {
			if (!"Y".equals(f.getUseYn())) continue;
			out.add(toMap(f));
		}
		return out;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listForAdmin(String propCd) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (ConciergeFeature f : featureRepo.findByPropCdOrderBySortOrdAsc(propCd)) {
			out.add(toMap(f));
		}
		return out;
	}

	@Transactional
	public List<Map<String, Object>> upsertBulk(String propCd, List<Map<String, Object>> rows) {
		LocalDateTime now = LocalDateTime.now();
		for (Map<String, Object> row : rows) {
			String featureCd = (String) row.get("featureCd");
			if (featureCd == null || featureCd.isBlank()) continue;
			ConciergeFeature f = featureRepo.findById(new ConciergeFeatureId(propCd, featureCd))
					.orElseGet(() -> new ConciergeFeature(propCd, featureCd, "N", 99));
			Object useYn = row.get("useYn");
			if (useYn instanceof String s) f.setUseYn(s);
			Object sortOrd = row.get("sortOrd");
			if (sortOrd instanceof Number n) f.setSortOrd(n.intValue());
			Object cfg = row.get("configJson");
			if (cfg instanceof String s) f.setConfigJson(s);
			f.setUpdUser("ADMIN");
			f.setUpdDt(now);
			featureRepo.save(f);
		}
		return listForAdmin(propCd);
	}

	private Map<String, Object> toMap(ConciergeFeature f) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("propCd", f.getPropCd());
		m.put("featureCd", f.getFeatureCd());
		m.put("useYn", f.getUseYn());
		m.put("sortOrd", f.getSortOrd());
		m.put("configJson", f.getConfigJson());
		return m;
	}
}
