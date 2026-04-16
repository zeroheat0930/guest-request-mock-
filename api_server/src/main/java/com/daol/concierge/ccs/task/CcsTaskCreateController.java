package com.daol.concierge.ccs.task;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.ccs.service.CcsTaskService;
import com.daol.concierge.ccs.util.CcsResponse;
import com.daol.concierge.core.api.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ccs/tasks")
public class CcsTaskCreateController {

	@Autowired private CcsTaskService taskService;

	@PostMapping
	public Map<String, Object> create(@RequestBody Map<String, Object> body) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();

		String toDeptCd = str(body.get("toDeptCd"));
		String toAssigneeId = str(body.get("toAssigneeId"));
		String title = str(body.get("title"));
		String memo = str(body.get("memo"));
		String roomNo = str(body.get("roomNo"));

		if (toDeptCd == null || toDeptCd.isBlank() || title == null || title.isBlank())
			throw new BizException("9001", "필수값 누락");

		Map<String, Object> task = taskService.createTask(
				me.propCd(), me.cmpxCd(), "STAFF_REQ", me.staffId(),
				toDeptCd, title, memo, roomNo);

		if (toAssigneeId != null && !toAssigneeId.isBlank()) {
			String taskId = str(task.get("taskId"));
			task = taskService.assignTo(taskId, toAssigneeId);
		}

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("taskId", str(task.get("taskId")));
		map.put("statusCd", str(task.get("statusCd")));
		return CcsResponse.ok(map);
	}

	private static String str(Object o) {
		if (o == null) return null;
		String s = String.valueOf(o).strip();
		return s.isEmpty() ? null : s;
	}
}
