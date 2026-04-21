package com.daol.concierge.ccs.service;

import com.daol.concierge.ccs.audit.AuditLogService;
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
 * 분실물(Lost &amp; Found) 서비스 — CCS 자체 소유 도메인.
 *
 * 저장 순서: INV.CCS_LOSTFOUND 저장 → PmsSyncAdapter 로 PMS 전파(옵션) → WebSocket publish.
 * 게스트 신고/스태프 습득 모두 같은 테이블을 쓰고 REPORTER_TYPE 로 구분.
 */
@Service
public class CcsLostFoundService {

	@Autowired private InvMapper invMapper;
	@Autowired private PmsSyncAdapter pmsSyncAdapter;
	@Autowired(required = false) SimpMessagingTemplate messagingTemplate;
	@Autowired(required = false) AuditLogService auditLogService;

	public Map<String, Object> createReport(Map<String, Object> body) {
		String lfId = "LF" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
		Map<String, Object> p = new HashMap<>(body);
		p.put("lfId", lfId);
		p.putIfAbsent("statusCd", "REPORTED");
		if (!p.containsKey("reporterType")) p.put("reporterType", "GUEST");
		require(p, "propCd", "cmpxCd", "category", "itemName");
		invMapper.insertLostFound(p);
		Map<String, Object> saved = invMapper.selectLostFound(lfId);
		try { pmsSyncAdapter.syncLostFound(saved); } catch (Exception ignore) {}
		if (auditLogService != null) auditLogService.log(str(p.get("reporterRef")),
				str(p.get("reporterType")), "CREATE", "LOSTFOUND", lfId,
				str(p.get("propCd")), str(p.get("cmpxCd")), null, saved);
		publish(saved);
		return saved;
	}

	public List<Map<String, Object>> list(Map<String, Object> filter) {
		return invMapper.selectLostFoundList(filter);
	}

	public Map<String, Object> get(String lfId) {
		Map<String, Object> row = invMapper.selectLostFound(lfId);
		if (row == null) throw new ApiException(ApiStatus.NOT_FOUND, "분실물 없음");
		return row;
	}

	public Map<String, Object> updateStatus(String lfId, String statusCd, String handlerId, String note) {
		Map<String, Object> before = invMapper.selectLostFound(lfId);
		Map<String, Object> p = new HashMap<>();
		p.put("lfId", lfId);
		p.put("statusCd", statusCd);
		p.put("handlerId", handlerId);
		p.put("note", note);
		invMapper.updateLostFoundStatus(p);
		Map<String, Object> saved = invMapper.selectLostFound(lfId);
		try { pmsSyncAdapter.syncLostFound(saved); } catch (Exception ignore) {}
		if (auditLogService != null) auditLogService.log(handlerId, "STAFF", "STATUS_CHANGE", "LOSTFOUND", lfId,
				str(saved != null ? saved.get("propCd") : null), str(saved != null ? saved.get("cmpxCd") : null), before, saved);
		publish(saved);
		return saved;
	}

	public Map<String, Object> match(String lfId, String matchedLfId, String handlerId) {
		Map<String, Object> p = new HashMap<>();
		p.put("lfId", lfId);
		p.put("matchedLfId", matchedLfId);
		p.put("handlerId", handlerId);
		invMapper.updateLostFoundMatch(p);
		Map<String, Object> saved = invMapper.selectLostFound(lfId);
		try { pmsSyncAdapter.syncLostFound(saved); } catch (Exception ignore) {}
		publish(saved);
		return saved;
	}

	private static void require(Map<String, Object> p, String... keys) {
		for (String k : keys) {
			Object v = p.get(k);
			if (v == null || String.valueOf(v).isBlank())
				throw new ApiException(ApiStatus.BAD_REQUEST, "필수값 누락: " + k);
		}
	}

	void publish(Map<String, Object> lf) {
		if (messagingTemplate == null || lf == null) return;
		String propCd = str(lf.get("propCd"));
		String cmpxCd = str(lf.get("cmpxCd"));
		String handlerId = str(lf.get("handlerId"));
		// 분실물은 FR(프론트데스크) 부서로 라우팅됨
		messagingTemplate.convertAndSend("/topic/ccs/dept/FR", lf);
		if (propCd != null && cmpxCd != null)
			messagingTemplate.convertAndSend("/topic/ccs/cmpx/" + propCd + "/" + cmpxCd, lf);
		if (propCd != null)
			messagingTemplate.convertAndSend("/topic/ccs/prop/" + propCd, lf);
		if (handlerId != null && !handlerId.isEmpty())
			messagingTemplate.convertAndSend("/topic/ccs/staff/" + handlerId, lf);
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
