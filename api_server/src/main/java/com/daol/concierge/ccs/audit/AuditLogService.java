package com.daol.concierge.ccs.audit;

import com.daol.concierge.inv.mapper.InvMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 감사 로그 서비스.
 * 각 도메인 서비스가 create/update/delete 후 `log(...)` 를 호출해 before/after 를 기록.
 * 실패해도 비즈니스 트랜잭션을 방해하지 않음 (fire-and-forget, try/catch).
 */
@Service
public class AuditLogService {

	private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
	private static final ObjectMapper JSON = new ObjectMapper();

	@Autowired private InvMapper invMapper;

	/**
	 * 감사 로그 기록.
	 * @param actorId 수행자 ID
	 * @param actorType GUEST|STAFF|ADMIN|SYSTEM
	 * @param action CREATE|UPDATE|DELETE|STATUS_CHANGE|LOGIN|LOGOUT
	 * @param domain TASK|LOSTFOUND|VOC|RENTAL|DUTY|FEATURE
	 * @param entityId 대상 엔티티 ID (nullable)
	 * @param propCd / cmpxCd — 멀티 테넌시 스코프
	 * @param before / after — 변경 전/후 객체 (JSON 직렬화)
	 */
	public void log(String actorId, String actorType, String action, String domain,
			String entityId, String propCd, String cmpxCd, Object before, Object after) {
		try {
			String auditId = "AL" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
			Map<String, Object> p = new HashMap<>();
			p.put("auditId", auditId);
			p.put("propCd", propCd);
			p.put("cmpxCd", cmpxCd);
			p.put("actorId", actorId);
			p.put("actorType", actorType != null ? actorType : "SYSTEM");
			p.put("action", action);
			p.put("domain", domain);
			p.put("entityId", entityId);
			p.put("beforeJson", toJson(before));
			p.put("afterJson", toJson(after));
			invMapper.insertAuditLog(p);
		} catch (Exception e) {
			// 감사 로그 실패는 절대 비즈니스 로직 막지 않음 — 경고만
			log.warn("[audit] 감사 로그 기록 실패: {}", e.getMessage());
		}
	}

	public List<Map<String, Object>> query(Map<String, Object> filter) {
		return invMapper.selectAuditLogList(filter);
	}

	private static String toJson(Object o) {
		if (o == null) return null;
		try {
			String s = JSON.writeValueAsString(o);
			return s.length() > 3900 ? s.substring(0, 3900) + "...(truncated)" : s;
		} catch (Exception e) {
			return String.valueOf(o);
		}
	}
}
