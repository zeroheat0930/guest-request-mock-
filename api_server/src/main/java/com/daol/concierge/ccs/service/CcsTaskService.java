package com.daol.concierge.ccs.service;

import com.daol.concierge.core.api.BizException;
import com.daol.concierge.inv.mapper.InvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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
		if (t == null) throw new BizException("9404", "작업 없음");
		if (!"REQ".equals(str(t.get("statusCd")))) throw new BizException("9005", "상태 전이 불가");

		Map<String, Object> param = new HashMap<>();
		param.put("taskId", taskId);
		param.put("assigneeId", assigneeId);
		param.put("statusCd", "ASSIGNED");
		invMapper.updateTaskAssignee(param);

		Map<String, Object> saved = invMapper.selectTask(taskId);
		publish(saved);
		return saved;
	}

	public Map<String, Object> transitionStatus(String taskId, String newStatusCd) {
		Map<String, Object> t = invMapper.selectTask(taskId);
		if (t == null) throw new BizException("9404", "작업 없음");
		String cur = str(t.get("statusCd"));

		boolean legal = false;
		if ("CANCELED".equals(newStatusCd)) {
			legal = "REQ".equals(cur) || "ASSIGNED".equals(cur) || "IN_PROG".equals(cur);
		} else if ("ASSIGNED".equals(newStatusCd)) {
			legal = "REQ".equals(cur);
		} else if ("IN_PROG".equals(newStatusCd)) {
			legal = "ASSIGNED".equals(cur);
		} else if ("DONE".equals(newStatusCd)) {
			legal = "IN_PROG".equals(cur);
		}
		if (!legal) throw new BizException("9005", "상태 전이 불가");

		Map<String, Object> param = new HashMap<>();
		param.put("taskId", taskId);
		param.put("statusCd", newStatusCd);
		invMapper.updateTaskStatus(param);

		Map<String, Object> saved = invMapper.selectTask(taskId);
		publish(saved);
		return saved;
	}

	public List<Map<String, Object>> listForDept(String propCd, String cmpxCd, String deptCd, String statusCd) {
		return invMapper.selectTasksByDept(propCd, cmpxCd, deptCd, statusCd);
	}

	private void publish(Map<String, Object> task) {
		if (messagingTemplate == null || task == null) return;
		String deptCd = str(task.get("deptCd"));
		String assigneeId = str(task.get("assigneeId"));
		if (deptCd != null) messagingTemplate.convertAndSend("/topic/ccs/dept/" + deptCd, task);
		if (assigneeId != null && !assigneeId.isEmpty())
			messagingTemplate.convertAndSend("/topic/ccs/staff/" + assigneeId, task);
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
