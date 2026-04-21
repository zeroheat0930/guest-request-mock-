package com.daol.concierge.ccs.service;

import com.daol.concierge.ccs.audit.AuditLogService;
import com.daol.concierge.ccs.routing.CcsSlaRules;
import com.daol.concierge.ccs.sync.PmsSyncAdapter;
import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.inv.mapper.InvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 고객 불만(VOC) 서비스.
 * URGENT 심각도는 에스컬레이션 토픽(/topic/ccs/esc/{propCd}) 으로 즉시 푸시 — 관리자 토스트.
 */
@Service
public class CcsVocService {

	@Autowired private InvMapper invMapper;
	@Autowired private PmsSyncAdapter pmsSyncAdapter;
	@Autowired(required = false) SimpMessagingTemplate messagingTemplate;
	@Autowired(required = false) AuditLogService auditLogService;

	public Map<String, Object> createReport(Map<String, Object> body) {
		String vocId = "VOC" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
		Map<String, Object> p = new HashMap<>(body);
		p.put("vocId", vocId);
		p.putIfAbsent("statusCd", "OPEN");
		p.putIfAbsent("severity", "NORMAL");
		require(p, "propCd", "cmpxCd", "category", "title", "content");
		invMapper.insertVoc(p);
		Map<String, Object> saved = invMapper.selectVoc(vocId);
		try { pmsSyncAdapter.syncVoc(saved); } catch (Exception ignore) {}
		if (auditLogService != null) auditLogService.log(str(p.get("rsvNo")), "GUEST", "CREATE", "VOC", vocId,
				str(p.get("propCd")), str(p.get("cmpxCd")), null, saved);
		publish(saved);
		return saved;
	}

	public List<Map<String, Object>> list(Map<String, Object> filter) {
		return invMapper.selectVocList(filter);
	}

	public Map<String, Object> get(String vocId) {
		Map<String, Object> row = invMapper.selectVoc(vocId);
		if (row == null) throw new ApiException(ApiStatus.NOT_FOUND, "VOC 없음");
		return row;
	}

	public Map<String, Object> updateStatus(String vocId, String statusCd, String handlerId) {
		Map<String, Object> p = new HashMap<>();
		p.put("vocId", vocId);
		p.put("statusCd", statusCd);
		p.put("handlerId", handlerId);
		invMapper.updateVocStatus(p);
		Map<String, Object> saved = invMapper.selectVoc(vocId);
		try { pmsSyncAdapter.syncVoc(saved); } catch (Exception ignore) {}
		publish(saved);
		return saved;
	}

	public Map<String, Object> resolve(String vocId, String resolution, String handlerId) {
		Map<String, Object> before = invMapper.selectVoc(vocId);
		Map<String, Object> p = new HashMap<>();
		p.put("vocId", vocId);
		p.put("resolution", resolution);
		p.put("handlerId", handlerId);
		invMapper.updateVocResolution(p);
		Map<String, Object> saved = invMapper.selectVoc(vocId);
		try { pmsSyncAdapter.syncVoc(saved); } catch (Exception ignore) {}
		if (auditLogService != null) auditLogService.log(handlerId, "STAFF", "UPDATE", "VOC", vocId,
				str(saved != null ? saved.get("propCd") : null), str(saved != null ? saved.get("cmpxCd") : null), before, saved);
		publish(saved);
		return saved;
	}

	public Map<String, Object> satisfaction(String vocId, int rating) {
		if (rating < 1 || rating > 5) throw new ApiException(ApiStatus.BAD_REQUEST, "만족도는 1~5");
		Map<String, Object> p = new HashMap<>();
		p.put("vocId", vocId);
		p.put("satisfaction", rating);
		invMapper.updateVocSatisfaction(p);
		return invMapper.selectVoc(vocId);
	}

	private static void require(Map<String, Object> p, String... keys) {
		for (String k : keys) {
			Object v = p.get(k);
			if (v == null || String.valueOf(v).isBlank())
				throw new ApiException(ApiStatus.BAD_REQUEST, "필수값 누락: " + k);
		}
	}

	void publish(Map<String, Object> voc) {
		if (messagingTemplate == null || voc == null) return;
		String propCd = str(voc.get("propCd"));
		String cmpxCd = str(voc.get("cmpxCd"));
		String handlerId = str(voc.get("handlerId"));
		String severity = str(voc.get("severity"));
		voc.put("slaMin", CcsSlaRules.defaultSlaMin("VOC"));
		messagingTemplate.convertAndSend("/topic/ccs/dept/FR", voc);
		if (propCd != null && cmpxCd != null)
			messagingTemplate.convertAndSend("/topic/ccs/cmpx/" + propCd + "/" + cmpxCd, voc);
		if (propCd != null)
			messagingTemplate.convertAndSend("/topic/ccs/prop/" + propCd, voc);
		if (handlerId != null && !handlerId.isEmpty())
			messagingTemplate.convertAndSend("/topic/ccs/staff/" + handlerId, voc);
		// URGENT 심각도는 관리자 에스컬레이션 토픽으로 즉시 푸시
		if ("URGENT".equals(severity) && propCd != null)
			messagingTemplate.convertAndSend("/topic/ccs/esc/" + propCd, voc);
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
