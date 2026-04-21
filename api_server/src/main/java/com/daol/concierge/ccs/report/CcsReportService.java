package com.daol.concierge.ccs.report;

import com.daol.concierge.ccs.routing.CcsSlaRules;
import com.daol.concierge.inv.mapper.InvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 리포트 서비스 — CCS_TASK 기반 집계.
 * selectDailyReport / selectSlaReport / selectHeatmapReport 의 결과를 후처리(SLA 비교 등) 해서 반환.
 */
@Service
public class CcsReportService {

	@Autowired private InvMapper invMapper;

	public List<Map<String, Object>> daily(Map<String, Object> filter) {
		List<Map<String, Object>> rows = invMapper.selectDailyReport(filter);
		if (rows != null) {
			for (Map<String, Object> r : rows) {
				Integer sla = CcsSlaRules.defaultSlaMin(str(r.get("sourceType")));
				r.put("slaMin", sla);
				Object avg = r.get("avgElapsedMin");
				if (avg instanceof Number) {
					r.put("slaCompliance", ((Number) avg).doubleValue() <= sla ? "MET" : "MISSED");
				}
			}
		}
		return rows;
	}

	public List<Map<String, Object>> sla(Map<String, Object> filter) {
		List<Map<String, Object>> rows = invMapper.selectSlaReport(filter);
		if (rows != null) {
			for (Map<String, Object> r : rows) {
				int sla = CcsSlaRules.defaultSlaMin(str(r.get("sourceType")));
				r.put("slaMin", sla);
				Object avg = r.get("avgElapsedMin");
				Object total = r.get("totalCount");
				Object done = r.get("doneCount");
				if (avg instanceof Number && total instanceof Number) {
					double t = ((Number) total).doubleValue();
					double d = done instanceof Number ? ((Number) done).doubleValue() : 0;
					r.put("completionRate", t > 0 ? Math.round((d / t) * 1000.0) / 10.0 : 0);
					r.put("slaMet", ((Number) avg).doubleValue() <= sla);
				}
			}
		}
		return rows;
	}

	public List<Map<String, Object>> heatmap(Map<String, Object> filter) {
		return invMapper.selectHeatmapReport(filter);
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
