package com.daol.concierge.ccs.service;

import com.daol.concierge.ccs.sync.PmsSyncAdapter;
import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.inv.mapper.InvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 당직 로그 — 교대 시작/핸드오버/종료 + 사건 타임라인.
 * PMS 야간감사 상태는 `selectPmsNightAuditStatus` 로 읽기 전용 조회(현재 placeholder).
 */
@Service
public class CcsDutyService {

	@Autowired private InvMapper invMapper;
	@Autowired private PmsSyncAdapter pmsSyncAdapter;
	@Autowired(required = false) SimpMessagingTemplate messagingTemplate;

	public Map<String, Object> startShift(Map<String, Object> body) {
		require(body, "propCd", "cmpxCd", "shiftCd", "managerId");
		String logId = "DL" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
		Map<String, Object> p = new HashMap<>(body);
		p.put("logId", logId);
		p.putIfAbsent("dutyDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
		p.putIfAbsent("pmsAuditDoneYn", "N");
		invMapper.insertDutyLog(p);
		Map<String, Object> saved = invMapper.selectDutyLog(logId);
		try { pmsSyncAdapter.syncDuty(saved); } catch (Exception ignore) {}
		publish(saved);
		return saved;
	}

	public List<Map<String, Object>> list(Map<String, Object> filter) {
		return invMapper.selectDutyLogList(filter);
	}

	public Map<String, Object> today(String propCd, String cmpxCd, String shift) {
		Map<String, Object> row = invMapper.selectDutyLogToday(propCd, cmpxCd, shift);
		Map<String, Object> pmsStat = invMapper.selectPmsNightAuditStatus(propCd,
				LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
		Map<String, Object> result = new HashMap<>();
		result.put("log", row);
		result.put("pmsAudit", pmsStat);
		return result;
	}

	public Map<String, Object> handover(String logId, Map<String, Object> body) {
		Map<String, Object> o = invMapper.selectDutyLog(logId);
		if (o == null) throw new ApiException(ApiStatus.NOT_FOUND, "당직 로그 없음");
		Map<String, Object> p = new HashMap<>(body);
		p.put("logId", logId);
		invMapper.updateDutyLogHandover(p);
		Map<String, Object> saved = invMapper.selectDutyLog(logId);
		try { pmsSyncAdapter.syncDuty(saved); } catch (Exception ignore) {}
		publish(saved);
		return saved;
	}

	public Map<String, Object> close(String logId, Map<String, Object> body) {
		Map<String, Object> o = invMapper.selectDutyLog(logId);
		if (o == null) throw new ApiException(ApiStatus.NOT_FOUND, "당직 로그 없음");
		Map<String, Object> p = new HashMap<>(body);
		p.put("logId", logId);
		invMapper.updateDutyLogClose(p);
		Map<String, Object> saved = invMapper.selectDutyLog(logId);
		try { pmsSyncAdapter.syncDuty(saved); } catch (Exception ignore) {}
		publish(saved);
		return saved;
	}

	public void delete(String logId) {
		Map<String, Object> o = invMapper.selectDutyLog(logId);
		if (o == null) throw new ApiException(ApiStatus.NOT_FOUND, "당직 로그 없음");
		invMapper.deleteDutyLog(logId);
		publish(o);
	}

	private static void require(Map<String, Object> p, String... keys) {
		for (String k : keys) {
			Object v = p.get(k);
			if (v == null || String.valueOf(v).isBlank())
				throw new ApiException(ApiStatus.BAD_REQUEST, "필수값 누락: " + k);
		}
	}

	void publish(Map<String, Object> log) {
		if (messagingTemplate == null || log == null) return;
		String propCd = str(log.get("propCd"));
		String cmpxCd = str(log.get("cmpxCd"));
		// 당직은 관리자 전용 — prop/cmpx 토픽으로만 푸시
		if (propCd != null && cmpxCd != null)
			messagingTemplate.convertAndSend("/topic/ccs/cmpx/" + propCd + "/" + cmpxCd, log);
		if (propCd != null)
			messagingTemplate.convertAndSend("/topic/ccs/prop/" + propCd, log);
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
