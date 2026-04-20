package com.daol.concierge.feature;

import com.daol.concierge.inv.mapper.InvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeatureService {

	@Autowired private InvMapper invMapper;

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
			param.put("configJson", row.get("configJson"));
			param.put("regUser", "ADMIN");
			param.put("modUser", "ADMIN");

			int updated = invMapper.updateFeature(param);
			if (updated == 0) {
				invMapper.insertFeature(param);
			}
		}
		return listForAdmin(propCd, cmpxCd);
	}
}
