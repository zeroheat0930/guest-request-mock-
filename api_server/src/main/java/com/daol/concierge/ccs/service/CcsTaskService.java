package com.daol.concierge.ccs.service;

import com.daol.concierge.ccs.routing.CcsSlaRules;
import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.inv.mapper.InvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CcsTaskService {

	@Autowired private InvMapper invMapper;
	@Autowired(required = false) SimpMessagingTemplate messagingTemplate;

	public Map<String, Object> createTask(String propCd, String cmpxCd, String sourceType,
			String sourceRefNo, String deptCd, String title, String memo, String roomNo) {
		String taskId = "CT" + System.currentTimeMillis()
				+ String.format("%04d", ThreadLocalRandom.current().nextInt(10000));

		Map<String, Object> t = new HashMap<>();
		t.put("taskId", taskId);
		t.put("propCd", propCd);
		t.put("cmpxCd", cmpxCd);
		t.put("sourceType", sourceType);
		t.put("sourceRefNo", sourceRefNo);
		t.put("deptCd", deptCd);
		t.put("title", title);
		t.put("memo", memo);
		t.put("rmNo", roomNo);
		t.put("assigneeId", null);
		t.put("statusCd", "REQ");
		invMapper.insertTask(t);

		Map<String, Object> saved = invMapper.selectTask(taskId);
		publish(saved);
		return saved;
	}

	public Map<String, Object> assignTo(String taskId, String assigneeId) {
		Map<String, Object> t = invMapper.selectTask(taskId);
		if (t == null) throw new ApiException(ApiStatus.NOT_FOUND, "작업 없음");
		if (!"REQ".equals(str(t.get("statusCd")))) throw new ApiException(ApiStatus.SYSTEM_ERROR, "상태 전이 불가");

		Map<String, Object> param = new HashMap<>();
		param.put("taskId", taskId);
		param.put("assigneeId", assigneeId);
		param.put("statusCd", "IN_PROG");
		invMapper.updateTaskAssignee(param);

		Map<String, Object> saved = invMapper.selectTask(taskId);
		publish(saved);
		return saved;
	}

	public Map<String, Object> transitionStatus(String taskId, String newStatusCd) {
		Map<String, Object> t = invMapper.selectTask(taskId);
		if (t == null) throw new ApiException(ApiStatus.NOT_FOUND, "작업 없음");
		String cur = str(t.get("statusCd"));

		boolean legal = false;
		if ("CANCELED".equals(newStatusCd)) {
			legal = "REQ".equals(cur) || "ASSIGNED".equals(cur) || "IN_PROG".equals(cur);
		} else if ("IN_PROG".equals(newStatusCd)) {
			// REQ 에서 바로 시작(대시보드 내가 받기 경로) + ASSIGNED 에서 시작(러너 시작 버튼) 양쪽 허용
			legal = "REQ".equals(cur) || "ASSIGNED".equals(cur);
		} else if ("DONE".equals(newStatusCd)) {
			legal = "IN_PROG".equals(cur);
		}
		if (!legal) throw new ApiException(ApiStatus.SYSTEM_ERROR, "상태 전이 불가");

		Map<String, Object> param = new HashMap<>();
		param.put("taskId", taskId);
		param.put("statusCd", newStatusCd);
		invMapper.updateTaskStatus(param);

		Map<String, Object> saved = invMapper.selectTask(taskId);
		publish(saved);
		return saved;
	}

	public List<Map<String, Object>> listForDept(String propCd, String cmpxCd, String deptCd, String statusCd) {
		List<Map<String, Object>> list = invMapper.selectTasksByDept(propCd, cmpxCd, deptCd, statusCd);
		if (list != null) list.forEach(this::enrichSla);
		return list;
	}

	/**
	 * 태스크 응답에 SLA 관련 필드를 주입한다.
	 *  - slaMin    : 이 sourceType 의 기본 SLA 분
	 *  - elapsedMin: 생성 후 경과한 분
	 *  - overdue   : 열린(REQ/ASSIGNED/IN_PROG) 상태에서 elapsedMin > slaMin 이면 true
	 */
	void enrichSla(Map<String, Object> task) {
		if (task == null) return;
		String srcType  = str(task.get("sourceType"));
		String statusCd = str(task.get("statusCd"));
		int sla = CcsSlaRules.defaultSlaMin(srcType);
		task.put("slaMin", sla);
		long elapsed = 0;
		Object c = task.get("createdAt");
		try {
			LocalDateTime created = toLocalDateTime(c);
			if (created != null) elapsed = Math.max(0, Duration.between(created, LocalDateTime.now()).toMinutes());
		} catch (Exception ignore) {}
		task.put("elapsedMin", elapsed);
		boolean open = "REQ".equals(statusCd) || "ASSIGNED".equals(statusCd) || "IN_PROG".equals(statusCd);
		task.put("overdue", open && elapsed > sla);
	}

	private static LocalDateTime toLocalDateTime(Object o) {
		if (o == null) return null;
		if (o instanceof LocalDateTime) return (LocalDateTime) o;
		if (o instanceof Timestamp) return ((Timestamp) o).toLocalDateTime();
		String s = o.toString().trim();
		// MariaDB 가 "yyyy-MM-dd HH:mm:ss[.fff]" 로 내려오는 경우
		if (s.contains(" ")) s = s.replace(' ', 'T');
		// 소수점 제거 (ISO 파서가 마이크로초 처리를 까다롭게 함)
		int dot = s.indexOf('.');
		if (dot > 0) s = s.substring(0, dot);
		try {
			return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 역할 기반 토픽 구조로 다중 publish:
	 *  - /topic/ccs/dept/{deptCd}               일반 사용자 / 부서장 / 러너
	 *  - /topic/ccs/cmpx/{propCd}/{cmpxCd}       컴플렉스 관리자(USER_TP=00003)
	 *  - /topic/ccs/prop/{propCd}                프로퍼티/시스템 관리자(USER_TP=00001,00002)
	 *  - /topic/ccs/staff/{assigneeId}            본인 배정 알림
	 *  - /topic/ccs/esc/{propCd}                  SLA 초과 에스컬레이션 (관리자 토스트)
	 */
	private void publish(Map<String, Object> task) {
		if (messagingTemplate == null || task == null) return;
		enrichSla(task);
		String deptCd = str(task.get("deptCd"));
		String propCd = str(task.get("propCd"));
		String cmpxCd = str(task.get("cmpxCd"));
		String assigneeId = str(task.get("assigneeId"));
		if (deptCd != null) messagingTemplate.convertAndSend("/topic/ccs/dept/" + deptCd, task);
		if (propCd != null && cmpxCd != null)
			messagingTemplate.convertAndSend("/topic/ccs/cmpx/" + propCd + "/" + cmpxCd, task);
		if (propCd != null)
			messagingTemplate.convertAndSend("/topic/ccs/prop/" + propCd, task);
		if (assigneeId != null && !assigneeId.isEmpty())
			messagingTemplate.convertAndSend("/topic/ccs/staff/" + assigneeId, task);
		// SLA 초과 에스컬레이션 — 관리자 전용 별도 토픽
		if (Boolean.TRUE.equals(task.get("overdue")) && propCd != null)
			messagingTemplate.convertAndSend("/topic/ccs/esc/" + propCd, task);
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
